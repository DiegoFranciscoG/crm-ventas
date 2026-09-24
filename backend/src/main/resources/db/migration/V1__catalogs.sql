-- Catálogos oficiales y de negocio. Fuentes: docs/investigacion.md (#6, #7, #9, #25, #1 Art. 7-8).

CREATE TABLE roles (
    code        VARCHAR(20)  PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL
);

INSERT INTO roles (code, name) VALUES
    ('ADMIN',     'Administrador'),
    ('MANAGER',   'Gerente comercial'),
    ('SALES_REP', 'Vendedor');

-- INEC, División Político Administrativa (#6)
CREATE TABLE provinces (
    code        VARCHAR(2)   PRIMARY KEY CHECK (code ~ '^[0-9]{2}$'),
    name        VARCHAR(60)  NOT NULL
);

INSERT INTO provinces (code, name) VALUES
    ('01', 'Azuay'), ('02', 'Bolívar'), ('03', 'Cañar'), ('04', 'Carchi'),
    ('05', 'Cotopaxi'), ('06', 'Chimborazo'), ('07', 'El Oro'), ('08', 'Esmeraldas'),
    ('09', 'Guayas'), ('10', 'Imbabura'), ('11', 'Loja'), ('12', 'Los Ríos'),
    ('13', 'Manabí'), ('14', 'Morona Santiago'), ('15', 'Napo'), ('16', 'Pastaza'),
    ('17', 'Pichincha'), ('18', 'Tungurahua'), ('19', 'Zamora Chinchipe'), ('20', 'Galápagos'),
    ('21', 'Sucumbíos'), ('22', 'Orellana'), ('23', 'Santo Domingo de los Tsáchilas'),
    ('24', 'Santa Elena'), ('90', 'Zonas no delimitadas');

-- INEC, CIIU Rev. 4.0, secciones (#25)
CREATE TABLE industries (
    code        VARCHAR(1)   PRIMARY KEY CHECK (code ~ '^[A-U]$'),
    name        VARCHAR(200) NOT NULL
);

INSERT INTO industries (code, name) VALUES
    ('A', 'Agricultura, ganadería, silvicultura y pesca'),
    ('B', 'Explotación de minas y canteras'),
    ('C', 'Industrias manufactureras'),
    ('D', 'Suministro de electricidad, gas, vapor y aire acondicionado'),
    ('E', 'Distribución de agua; alcantarillado, gestión de desechos y actividades de saneamiento'),
    ('F', 'Construcción'),
    ('G', 'Comercio al por mayor y al por menor; reparación de vehículos automotores y motocicletas'),
    ('H', 'Transporte y almacenamiento'),
    ('I', 'Actividades de alojamiento y de servicio de comidas'),
    ('J', 'Información y comunicación'),
    ('K', 'Actividades financieras y de seguros'),
    ('L', 'Actividades inmobiliarias'),
    ('M', 'Actividades profesionales, científicas y técnicas'),
    ('N', 'Actividades de servicios administrativos y de apoyo'),
    ('O', 'Administración pública y defensa; planes de seguridad social de afiliación obligatoria'),
    ('P', 'Enseñanza'),
    ('Q', 'Actividades de atención de la salud humana y de asistencia social'),
    ('R', 'Artes, entretenimiento y recreación'),
    ('S', 'Otras actividades de servicios'),
    ('T', 'Actividades de los hogares como empleadores y como productores de bienes y servicios para uso propio'),
    ('U', 'Actividades de organizaciones y órganos extraterritoriales');

-- SRI, Ficha técnica de comprobantes electrónicos v2.32, Tabla 17 (#7). IVA 15 % vigente (#8).
CREATE TABLE vat_rates (
    code        VARCHAR(2)    PRIMARY KEY,
    description VARCHAR(60)   NOT NULL,
    percentage  NUMERIC(5, 2) NOT NULL CHECK (percentage BETWEEN 0 AND 100),
    active      BOOLEAN       NOT NULL
);

INSERT INTO vat_rates (code, description, percentage, active) VALUES
    ('0',  'IVA 0 %',               0.00,  TRUE),
    ('2',  'IVA 12 %',              12.00, FALSE),
    ('3',  'IVA 14 %',              14.00, FALSE),
    ('4',  'IVA 15 %',              15.00, TRUE),
    ('5',  'IVA 5 %',               5.00,  TRUE),
    ('6',  'No objeto de impuesto', 0.00,  TRUE),
    ('7',  'Exento de IVA',         0.00,  TRUE),
    ('8',  'IVA diferenciado',      0.00,  FALSE),
    ('10', 'IVA 13 %',              13.00, FALSE);

-- Etapas del pipeline con probabilidad y categoría de forecast (#9, #10)
CREATE TABLE pipeline_stages (
    code                VARCHAR(30) PRIMARY KEY,
    name                VARCHAR(60) NOT NULL,
    position            SMALLINT    NOT NULL UNIQUE,
    default_probability SMALLINT    NOT NULL CHECK (default_probability BETWEEN 0 AND 100),
    forecast_category   VARCHAR(20) NOT NULL
        CHECK (forecast_category IN ('PIPELINE', 'BEST_CASE', 'COMMIT', 'CLOSED', 'OMITTED')),
    closed              BOOLEAN     NOT NULL,
    won                 BOOLEAN     NOT NULL,
    CONSTRAINT ck_stage_won_is_closed CHECK (NOT won OR closed)
);

INSERT INTO pipeline_stages (code, name, position, default_probability, forecast_category, closed, won) VALUES
    ('PROSPECTING',       'Prospección',        1, 10,  'PIPELINE',  FALSE, FALSE),
    ('QUALIFICATION',     'Calificación',       2, 20,  'PIPELINE',  FALSE, FALSE),
    ('VALUE_PROPOSITION', 'Propuesta de valor', 3, 50,  'PIPELINE',  FALSE, FALSE),
    ('PROPOSAL',          'Cotización enviada', 4, 75,  'BEST_CASE', FALSE, FALSE),
    ('NEGOTIATION',       'Negociación',        5, 90,  'COMMIT',    FALSE, FALSE),
    ('CLOSED_WON',        'Ganada',             6, 100, 'CLOSED',    TRUE,  TRUE),
    ('CLOSED_LOST',       'Perdida',            7, 0,   'OMITTED',   TRUE,  FALSE);

-- Finalidades del tratamiento (LOPDP Art. 7 y 8 #1; plazo de conservación, Reglamento Art. 10 #2; S4)
CREATE TABLE processing_purposes (
    code             VARCHAR(30)  PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(500) NOT NULL,
    legal_basis      VARCHAR(30)  NOT NULL
        CHECK (legal_basis IN ('CONSENT', 'LEGAL_OBLIGATION', 'COURT_ORDER', 'PUBLIC_INTEREST',
                               'CONTRACT', 'VITAL_INTEREST', 'PUBLIC_DATABASE', 'LEGITIMATE_INTEREST')),
    retention_months SMALLINT     NOT NULL CHECK (retention_months > 0)
);

INSERT INTO processing_purposes (code, name, description, legal_basis, retention_months) VALUES
    ('COMMERCIAL_CONTACT', 'Contacto comercial y seguimiento',
     'Registrar al contacto como interlocutor de su empresa y contactarlo por llamadas, reuniones o correo para dar seguimiento a oportunidades de venta.',
     'CONSENT', 24),
    ('QUOTE_DELIVERY', 'Envío de cotizaciones',
     'Preparar y enviar cotizaciones solicitadas por el contacto a nombre de su empresa (medidas precontractuales a petición del titular).',
     'CONTRACT', 36),
    ('EMAIL_MARKETING', 'Mercadotecnia por correo',
     'Enviar novedades comerciales y promociones. El titular puede oponerse en cualquier momento (LOPDP Art. 16 num. 2).',
     'CONSENT', 12);
