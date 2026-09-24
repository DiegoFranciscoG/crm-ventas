-- Usuarios, roles y log de accesos a datos personales (LOPDP Art. 10 k #1; OWASP A09 #20).

CREATE TABLE users (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role_code     VARCHAR(20)  NOT NULL REFERENCES roles (code),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);

CREATE UNIQUE INDEX ux_users_email ON users (email);

-- Solo IDs y metadatos: sin datos personales del titular en el detalle.
CREATE TABLE access_audit_log (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    user_id       BIGINT       NOT NULL REFERENCES users (id),
    action        VARCHAR(30)  NOT NULL
        CHECK (action IN ('VIEW', 'LIST', 'CREATE', 'UPDATE', 'EXPORT', 'ERASE',
                          'CONSENT_GRANT', 'CONSENT_REVOKE', 'PDF_DOWNLOAD')),
    resource_type VARCHAR(30)  NOT NULL
        CHECK (resource_type IN ('CONTACT', 'COMPANY', 'QUOTE', 'DATA_SUBJECT_REQUEST')),
    resource_id   BIGINT,
    success       BOOLEAN      NOT NULL,
    ip_address    VARCHAR(45),
    detail        VARCHAR(200)
);

CREATE INDEX ix_audit_occurred_at ON access_audit_log (occurred_at DESC);
CREATE INDEX ix_audit_resource ON access_audit_log (resource_type, resource_id);
CREATE INDEX ix_audit_user ON access_audit_log (user_id);

-- El log es evidencia: solo se permite insertar.
CREATE FUNCTION reject_audit_log_mutation() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'access_audit_log is append-only';
END;
$$;

CREATE TRIGGER trg_access_audit_log_no_update_delete
    BEFORE UPDATE OR DELETE ON access_audit_log
    FOR EACH ROW EXECUTE FUNCTION reject_audit_log_mutation();

CREATE TRIGGER trg_access_audit_log_no_truncate
    BEFORE TRUNCATE ON access_audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION reject_audit_log_mutation();
