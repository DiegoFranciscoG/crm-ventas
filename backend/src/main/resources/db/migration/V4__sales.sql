-- Productos, oportunidades, actividades y cotizaciones (#7, #9, #10).

CREATE TABLE products (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku           VARCHAR(40)   NOT NULL,
    name          VARCHAR(200)  NOT NULL,
    description   VARCHAR(1000),
    unit_price    NUMERIC(14, 2) NOT NULL CHECK (unit_price >= 0),
    vat_rate_code VARCHAR(2)    NOT NULL REFERENCES vat_rates (code),
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_products_sku ON products (upper(sku));

CREATE TABLE opportunities (
    id                  BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title               VARCHAR(200)   NOT NULL,
    company_id          BIGINT         NOT NULL REFERENCES companies (id) ON DELETE RESTRICT,
    primary_contact_id  BIGINT         REFERENCES contacts (id) ON DELETE SET NULL,
    owner_id            BIGINT         NOT NULL REFERENCES users (id),
    stage_code          VARCHAR(30)    NOT NULL REFERENCES pipeline_stages (code),
    amount              NUMERIC(14, 2) NOT NULL CHECK (amount >= 0),
    probability         SMALLINT       NOT NULL CHECK (probability BETWEEN 0 AND 100),
    expected_close_date DATE           NOT NULL,
    closed_at           TIMESTAMPTZ,
    lost_reason         VARCHAR(300),
    description         VARCHAR(2000),
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX ix_opportunities_owner_stage ON opportunities (owner_id, stage_code);
CREATE INDEX ix_opportunities_close_date ON opportunities (expected_close_date);
CREATE INDEX ix_opportunities_company ON opportunities (company_id);

CREATE TABLE opportunity_stage_history (
    id             BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    opportunity_id BIGINT         NOT NULL REFERENCES opportunities (id) ON DELETE CASCADE,
    from_stage     VARCHAR(30)    REFERENCES pipeline_stages (code),
    to_stage       VARCHAR(30)    NOT NULL REFERENCES pipeline_stages (code),
    amount         NUMERIC(14, 2) NOT NULL,
    probability    SMALLINT       NOT NULL,
    changed_by     BIGINT         NOT NULL REFERENCES users (id),
    changed_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX ix_stage_history_opportunity ON opportunity_stage_history (opportunity_id, changed_at);

CREATE TABLE activities (
    id               BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type             VARCHAR(20)   NOT NULL CHECK (type IN ('CALL', 'MEETING', 'EMAIL', 'TASK', 'NOTE')),
    subject          VARCHAR(200)  NOT NULL,
    notes            VARCHAR(4000),
    due_at           TIMESTAMPTZ,
    remind_at        TIMESTAMPTZ,
    reminder_sent_at TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    owner_id         BIGINT        NOT NULL REFERENCES users (id),
    company_id       BIGINT        REFERENCES companies (id) ON DELETE CASCADE,
    contact_id       BIGINT        REFERENCES contacts (id) ON DELETE SET NULL,
    opportunity_id   BIGINT        REFERENCES opportunities (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_activity_has_parent CHECK (company_id IS NOT NULL OR opportunity_id IS NOT NULL),
    CONSTRAINT ck_activity_reminder_before_due CHECK (remind_at IS NULL OR due_at IS NULL OR remind_at <= due_at)
);

CREATE INDEX ix_activities_owner_due ON activities (owner_id, due_at);
CREATE INDEX ix_activities_opportunity ON activities (opportunity_id);
-- Solo los recordatorios pendientes: el job de envío recorre este índice.
CREATE INDEX ix_activities_pending_reminders ON activities (remind_at)
    WHERE reminder_sent_at IS NULL AND completed_at IS NULL AND remind_at IS NOT NULL;

CREATE SEQUENCE quote_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE quotes (
    id             BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    number         VARCHAR(20)    NOT NULL,
    opportunity_id BIGINT         NOT NULL REFERENCES opportunities (id) ON DELETE CASCADE,
    contact_id     BIGINT         REFERENCES contacts (id) ON DELETE SET NULL,
    status         VARCHAR(20)    NOT NULL CHECK (status IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    issue_date     DATE           NOT NULL,
    valid_until    DATE           NOT NULL,
    currency       VARCHAR(3)     NOT NULL DEFAULT 'USD',
    subtotal       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_total NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_total      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    notes          VARCHAR(2000),
    created_by     BIGINT         NOT NULL REFERENCES users (id),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_quote_validity CHECK (valid_until >= issue_date)
);

CREATE UNIQUE INDEX ux_quotes_number ON quotes (number);
CREATE INDEX ix_quotes_opportunity ON quotes (opportunity_id);

CREATE TABLE quote_items (
    id               BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quote_id         BIGINT         NOT NULL REFERENCES quotes (id) ON DELETE CASCADE,
    line_number      SMALLINT       NOT NULL,
    product_id       BIGINT         REFERENCES products (id) ON DELETE SET NULL,
    description      VARCHAR(500)   NOT NULL,
    quantity         NUMERIC(12, 2) NOT NULL CHECK (quantity > 0),
    unit_price       NUMERIC(14, 2) NOT NULL CHECK (unit_price >= 0),
    discount_percent NUMERIC(5, 2)  NOT NULL DEFAULT 0 CHECK (discount_percent BETWEEN 0 AND 100),
    vat_rate_code    VARCHAR(2)     NOT NULL REFERENCES vat_rates (code),
    vat_percentage   NUMERIC(5, 2)  NOT NULL,
    line_subtotal    NUMERIC(14, 2) NOT NULL,
    line_discount    NUMERIC(14, 2) NOT NULL,
    line_tax         NUMERIC(14, 2) NOT NULL,
    line_total       NUMERIC(14, 2) NOT NULL,
    CONSTRAINT ux_quote_items_line UNIQUE (quote_id, line_number)
);
