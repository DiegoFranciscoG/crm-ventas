-- Empresas, contactos, consentimientos y solicitudes de derechos (LOPDP #1, Reglamento #2, SRI #3-#4).

CREATE TABLE companies (
    id                    BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruc                   VARCHAR(13)  NOT NULL CHECK (ruc ~ '^[0-9]{13}$'),
    taxpayer_type         VARCHAR(20)  NOT NULL
        CHECK (taxpayer_type IN ('NATURAL_PERSON', 'PUBLIC_ENTITY', 'PRIVATE_COMPANY')),
    ruc_checksum_verified BOOLEAN      NOT NULL,
    legal_name            VARCHAR(300) NOT NULL,
    trade_name            VARCHAR(300),
    industry_code         VARCHAR(1)   REFERENCES industries (code),
    province_code         VARCHAR(2)   NOT NULL REFERENCES provinces (code),
    city                  VARCHAR(100),
    address               VARCHAR(300),
    phone                 VARCHAR(20),
    email                 VARCHAR(254),
    website               VARCHAR(200),
    owner_id              BIGINT       NOT NULL REFERENCES users (id),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_companies_ruc ON companies (ruc);
CREATE INDEX ix_companies_owner ON companies (owner_id);
CREATE INDEX ix_companies_legal_name ON companies (lower(legal_name));

CREATE TABLE contacts (
    id                    BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id            BIGINT       NOT NULL REFERENCES companies (id) ON DELETE RESTRICT,
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    email                 VARCHAR(254),
    phone                 VARCHAR(20),
    job_title             VARCHAR(100),
    national_id_encrypted TEXT,
    data_source           VARCHAR(20)  NOT NULL
        CHECK (data_source IN ('DIRECT', 'REFERRAL', 'EVENT', 'PUBLIC_SOURCE')),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_contacts_email_lower CHECK (email IS NULL OR email = lower(email))
);

CREATE INDEX ix_contacts_company ON contacts (company_id);

CREATE TABLE contact_consents (
    id                     BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contact_id             BIGINT       NOT NULL REFERENCES contacts (id) ON DELETE CASCADE,
    purpose_code           VARCHAR(30)  NOT NULL REFERENCES processing_purposes (code),
    legal_basis            VARCHAR(30)  NOT NULL
        CHECK (legal_basis IN ('CONSENT', 'LEGAL_OBLIGATION', 'COURT_ORDER', 'PUBLIC_INTEREST',
                               'CONTRACT', 'VITAL_INTEREST', 'PUBLIC_DATABASE', 'LEGITIMATE_INTEREST')),
    channel                VARCHAR(20)  NOT NULL
        CHECK (channel IN ('WEB_FORM', 'EMAIL', 'IN_PERSON', 'PHONE_RECORDED', 'PAPER')),
    evidence               VARCHAR(500) NOT NULL,
    privacy_notice_version VARCHAR(20)  NOT NULL,
    granted_at             TIMESTAMPTZ  NOT NULL,
    expires_at             TIMESTAMPTZ  NOT NULL,
    revoked_at             TIMESTAMPTZ,
    revocation_reason      VARCHAR(300),
    recorded_by            BIGINT       NOT NULL REFERENCES users (id),
    CONSTRAINT ck_consent_expires_after_grant CHECK (expires_at > granted_at),
    CONSTRAINT ck_consent_revoked_after_grant CHECK (revoked_at IS NULL OR revoked_at >= granted_at)
);

-- Un solo registro vigente por contacto y finalidad (LOPDP Art. 8).
CREATE UNIQUE INDEX ux_consent_active_per_purpose
    ON contact_consents (contact_id, purpose_code) WHERE revoked_at IS NULL;

-- Registro de solicitudes de ejercicio de derechos (Reglamento Art. 15 #2). Sin FK: sobrevive a la eliminación.
CREATE TABLE data_subject_requests (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contact_id   BIGINT       NOT NULL,
    request_type VARCHAR(20)  NOT NULL
        CHECK (request_type IN ('ACCESS', 'RECTIFICATION', 'ERASURE', 'OPPOSITION', 'PORTABILITY', 'SUSPENSION')),
    channel      VARCHAR(20)  NOT NULL
        CHECK (channel IN ('EMAIL', 'WEB_FORM', 'IN_PERSON', 'PHONE', 'LETTER')),
    received_at  TIMESTAMPTZ  NOT NULL,
    due_at       TIMESTAMPTZ  NOT NULL,
    status       VARCHAR(20)  NOT NULL CHECK (status IN ('RECEIVED', 'COMPLETED', 'REJECTED')),
    completed_at TIMESTAMPTZ,
    resolution   VARCHAR(500),
    handled_by   BIGINT       NOT NULL REFERENCES users (id),
    CONSTRAINT ck_dsr_due_after_received CHECK (due_at >= received_at),
    CONSTRAINT ck_dsr_completed_has_date CHECK (status = 'RECEIVED' OR completed_at IS NOT NULL)
);

CREATE INDEX ix_dsr_contact ON data_subject_requests (contact_id);
CREATE INDEX ix_dsr_received ON data_subject_requests (received_at DESC);
