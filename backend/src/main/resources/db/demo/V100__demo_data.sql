-- Datos de demostración 100 % FICTICIOS. Solo se cargan con el perfil Spring "demo" (SPRING_PROFILES_ACTIVE=demo).
-- - Correos en example.com (RFC 2606, investigación #18): el sistema nunca envía correos reales a ese dominio.
-- - RUC sintéticos de sociedades en rangos altos de provincias pequeñas, para minimizar la colisión con RUC reales.
--   No hay RUC de personas naturales ni cédulas (LOPDP: no exponer identificaciones de personas en demos).
-- - Contraseña de los usuarios demo: Demo-CRM-2026 (publicada en el README; son cuentas sin rol ADMIN).

INSERT INTO users (email, full_name, password_hash, role_code) VALUES
    ('gerente@example.com',   'Gabriela Paredes', '$2b$12$sTW9Y8lwMbcf4vgzQH2/AeNT8SbSxKnX4ztilKu48jr0gJe1x.3Ne', 'MANAGER'),
    ('vendedor1@example.com', 'Andrés Villacís',  '$2b$12$sTW9Y8lwMbcf4vgzQH2/AeNT8SbSxKnX4ztilKu48jr0gJe1x.3Ne', 'SALES_REP'),
    ('vendedor2@example.com', 'Lucía Cevallos',   '$2b$12$sTW9Y8lwMbcf4vgzQH2/AeNT8SbSxKnX4ztilKu48jr0gJe1x.3Ne', 'SALES_REP');

INSERT INTO companies (ruc, taxpayer_type, ruc_checksum_verified, legal_name, trade_name, industry_code, province_code, city, address, phone, email, website, owner_id)
SELECT c.ruc, 'PRIVATE_COMPANY', c.verified, c.legal_name, c.trade_name, c.industry, c.province, c.city, c.address, c.phone, c.email, c.website,
       (SELECT id FROM users WHERE email = c.owner)
FROM (VALUES
    ('2099998701001', TRUE,  'Turismo Insular Galápagos Cía. Ltda.',     'Insular Tours',    'I', '20', 'Puerto Ayora',          'Av. Baltra y Floreana',        '05-000-0101', 'info@insular.example.com',     'https://insular.example.com',  'vendedor1@example.com'),
    ('2299984128001', TRUE,  'Servicios Petroleros Río Napo S.A.',       'PetroNapo',        'B', '22', 'Francisco de Orellana', 'Vía a Los Zorros km 3',        '06-000-0102', 'contacto@petronapo.example.com', NULL,                       'vendedor1@example.com'),
    ('2199976500001', TRUE,  'Agroindustrial Amazonía Verde S.A.',       'Amazonía Verde',   'A', '21', 'Nueva Loja',            'Av. Quito y Colombia',         '06-000-0103', 'ventas@amazoniaverde.example.com', NULL,                     'vendedor1@example.com'),
    ('1699965185001', TRUE,  'Maderas y Acabados Pastaza Cía. Ltda.',    'MadePastaza',      'C', '16', 'Puyo',                  'Vía Puyo-Tena km 2',           '03-000-0104', 'info@madepastaza.example.com', NULL,                         'vendedor2@example.com'),
    ('1999953472001', TRUE,  'Café de Altura Zamora S.A.',               'Café Zamora',      'C', '19', 'Zamora',                'Calle Diego de Vaca y Amazonas', '07-000-0105', 'hola@cafezamora.example.com', 'https://cafezamora.example.com', 'vendedor2@example.com'),
    ('1599942811001', TRUE,  'Logística Oriental Napo S.A.',             'LogiNapo',         'H', '15', 'Tena',                  'Av. 15 de Noviembre',          '06-000-0106', 'operaciones@loginapo.example.com', NULL,                     'vendedor2@example.com'),
    ('1499931758001', TRUE,  'Constructora Upano S.A.',                  NULL,               'F', '14', 'Macas',                 'Av. 29 de Mayo',               '07-000-0107', 'proyectos@upano.example.com', NULL,                          'vendedor1@example.com'),
    ('2199920637001', TRUE,  'Distribuidora Ferretera Lago Agrio S.A.',  'FerreLago',        'G', '21', 'Nueva Loja',            'Av. Circunvalación',           '06-000-0108', 'compras@ferrelago.example.com', NULL,                        'vendedor2@example.com'),
    ('1499919545001', TRUE,  'Tecnología Educativa Morona S.A.',         'EduMorona',        'P', '14', 'Macas',                 'Calle Bolívar y Soasti',       '07-000-0109', 'info@edumorona.example.com', 'https://edumorona.example.com', 'vendedor1@example.com'),
    ('2299981235001', FALSE, 'Soluciones Digitales Yasuní S.A.S.',       'Yasuní Digital',   'J', '22', 'Francisco de Orellana', 'Av. 9 de Octubre',             '06-000-0110', 'hola@yasuni.example.com', NULL,                              'vendedor2@example.com')
) AS c(ruc, verified, legal_name, trade_name, industry, province, city, address, phone, email, website, owner);

INSERT INTO contacts (company_id, first_name, last_name, email, phone, job_title, data_source)
SELECT (SELECT id FROM companies WHERE ruc = c.ruc), c.first_name, c.last_name, c.email, c.phone, c.job_title, c.source
FROM (VALUES
    ('2099998701001', 'María José', 'Andrade',  'maria.andrade@example.com',   '0990000101', 'Gerente de operaciones',   'DIRECT'),
    ('2099998701001', 'Pablo',      'Salazar',  'pablo.salazar@example.com',   '0990000102', 'Jefe de sistemas',         'DIRECT'),
    ('2299984128001', 'Carolina',   'Vega',     'carolina.vega@example.com',   '0990000103', 'Directora financiera',     'EVENT'),
    ('2299984128001', 'Jorge',      'Espinoza', 'jorge.espinoza@example.com',  '0990000104', 'Superintendente de campo', 'REFERRAL'),
    ('2199976500001', 'Valeria',    'Ortiz',    'valeria.ortiz@example.com',   '0990000105', 'Gerente general',          'DIRECT'),
    ('1699965185001', 'Fernando',   'Lozada',   'fernando.lozada@example.com', '0990000106', 'Gerente general',          'DIRECT'),
    ('1699965185001', 'Daniela',    'Carrión',  'daniela.carrion@example.com', '0990000107', 'Contadora',                'DIRECT'),
    ('1999953472001', 'Santiago',   'Jaramillo','santiago.jaramillo@example.com','0990000108','Gerente comercial',       'EVENT'),
    ('1599942811001', 'Paola',      'Quishpe',  'paola.quishpe@example.com',   '0990000109', 'Coordinadora logística',   'DIRECT'),
    ('1599942811001', 'Esteban',    'Mora',     'esteban.mora@example.com',    '0990000110', 'Jefe de flota',            'REFERRAL'),
    ('1499931758001', 'Gabriel',    'Tapia',    'gabriel.tapia@example.com',   '0990000111', 'Director de proyectos',    'EVENT'),
    ('2199920637001', 'Andrea',     'Benítez',  'andrea.benitez@example.com',  '0990000112', 'Gerente de compras',       'DIRECT'),
    ('1499919545001', 'Martín',     'Guerrero', 'martin.guerrero@example.com', '0990000113', 'Rector',                   'DIRECT'),
    ('1499919545001', 'Sofía',      'Naranjo',  'sofia.naranjo@example.com',   '0990000114', 'Coordinadora TIC',         'DIRECT'),
    ('2299981235001', 'Ricardo',    'Palacios', 'ricardo.palacios@example.com','0990000115', 'Gerente general',          'DIRECT'),
    ('1999953472001', 'Camila',     'Rosero',   'camila.rosero@example.com',   '0990000116', 'Jefa de marketing',        'EVENT')
) AS c(ruc, first_name, last_name, email, phone, job_title, source);

-- Consentimientos por finalidad, con evidencia (Reglamento Art. 5). Esteban Mora tiene el consentimiento vencido
-- (se otorgó hace 25 meses con conservación de 24) y Santiago Jaramillo revocó la mercadotecnia.
INSERT INTO contact_consents (contact_id, purpose_code, legal_basis, channel, evidence, privacy_notice_version, granted_at, expires_at, revoked_at, revocation_reason, recorded_by)
SELECT ct.id, x.purpose, p.legal_basis, x.channel, x.evidence, 'v1.0',
       now() - make_interval(days => x.days_ago),
       now() - make_interval(days => x.days_ago) + make_interval(months => p.retention_months),
       CASE WHEN x.revoked_days_ago IS NULL THEN NULL ELSE now() - make_interval(days => x.revoked_days_ago) END,
       x.revocation_reason,
       co.owner_id
FROM (VALUES
    ('maria.andrade@example.com',      'COMMERCIAL_CONTACT', 'WEB_FORM',  'Formulario web con aviso de privacidad v1.0 (registro DEMO-0001)', 60,  NULL, NULL),
    ('maria.andrade@example.com',      'QUOTE_DELIVERY',     'EMAIL',     'Correo de solicitud de cotización archivado (DEMO-0002)',          10,  NULL, NULL),
    ('maria.andrade@example.com',      'EMAIL_MARKETING',    'WEB_FORM',  'Casilla de suscripción marcada en formulario web (DEMO-0003)',     60,  NULL, NULL),
    ('pablo.salazar@example.com',      'COMMERCIAL_CONTACT', 'IN_PERSON', 'Formulario firmado en reunión inicial (DEMO-0004)',                45,  NULL, NULL),
    ('carolina.vega@example.com',      'COMMERCIAL_CONTACT', 'PAPER',     'Registro firmado en feria Expo Oriente (DEMO-0005)',                90,  NULL, NULL),
    ('carolina.vega@example.com',      'QUOTE_DELIVERY',     'EMAIL',     'Correo de solicitud de propuesta (DEMO-0006)',                     20,  NULL, NULL),
    ('jorge.espinoza@example.com',     'COMMERCIAL_CONTACT', 'EMAIL',     'Respuesta afirmativa a correo de presentación (DEMO-0007)',        30,  NULL, NULL),
    ('valeria.ortiz@example.com',      'COMMERCIAL_CONTACT', 'WEB_FORM',  'Formulario web con aviso de privacidad v1.0 (DEMO-0008)',          120, NULL, NULL),
    ('valeria.ortiz@example.com',      'QUOTE_DELIVERY',     'EMAIL',     'Correo de solicitud de cotización (DEMO-0009)',                    40,  NULL, NULL),
    ('fernando.lozada@example.com',    'COMMERCIAL_CONTACT', 'IN_PERSON', 'Formulario firmado en visita comercial (DEMO-0010)',               75,  NULL, NULL),
    ('fernando.lozada@example.com',    'QUOTE_DELIVERY',     'EMAIL',     'Correo de solicitud de cotización (DEMO-0011)',                    15,  NULL, NULL),
    ('daniela.carrion@example.com',    'COMMERCIAL_CONTACT', 'EMAIL',     'Respuesta afirmativa a correo (DEMO-0012)',                        50,  NULL, NULL),
    ('santiago.jaramillo@example.com', 'COMMERCIAL_CONTACT', 'PAPER',     'Registro firmado en feria del café (DEMO-0013)',                   100, NULL, NULL),
    ('santiago.jaramillo@example.com', 'EMAIL_MARKETING',    'PAPER',     'Casilla de suscripción marcada en registro de feria (DEMO-0014)',  100, 5,    'El titular solicitó no recibir promociones.'),
    ('paola.quishpe@example.com',      'COMMERCIAL_CONTACT', 'WEB_FORM',  'Formulario web con aviso de privacidad v1.0 (DEMO-0015)',          35,  NULL, NULL),
    ('paola.quishpe@example.com',      'QUOTE_DELIVERY',     'EMAIL',     'Correo de solicitud de propuesta (DEMO-0016)',                     12,  NULL, NULL),
    ('esteban.mora@example.com',       'COMMERCIAL_CONTACT', 'PHONE_RECORDED', 'Llamada grabada con lectura del aviso de privacidad (DEMO-0017)', 760, NULL, NULL),
    ('gabriel.tapia@example.com',      'COMMERCIAL_CONTACT', 'PAPER',     'Registro firmado en feria de la construcción (DEMO-0018)',         25,  NULL, NULL),
    ('andrea.benitez@example.com',     'COMMERCIAL_CONTACT', 'IN_PERSON', 'Formulario firmado en visita comercial (DEMO-0019)',               80,  NULL, NULL),
    ('andrea.benitez@example.com',     'QUOTE_DELIVERY',     'EMAIL',     'Correo de solicitud de cotización (DEMO-0020)',                    9,   NULL, NULL),
    ('martin.guerrero@example.com',    'COMMERCIAL_CONTACT', 'WEB_FORM',  'Formulario web con aviso de privacidad v1.0 (DEMO-0021)',          55,  NULL, NULL),
    ('sofia.naranjo@example.com',      'COMMERCIAL_CONTACT', 'EMAIL',     'Respuesta afirmativa a correo (DEMO-0022)',                        55,  NULL, NULL),
    ('ricardo.palacios@example.com',   'COMMERCIAL_CONTACT', 'IN_PERSON', 'Formulario firmado en reunión inicial (DEMO-0023)',                70,  NULL, NULL),
    ('ricardo.palacios@example.com',   'QUOTE_DELIVERY',     'EMAIL',     'Correo de solicitud de propuesta (DEMO-0024)',                     30,  NULL, NULL),
    ('camila.rosero@example.com',      'COMMERCIAL_CONTACT', 'WEB_FORM',  'Formulario web con aviso de privacidad v1.0 (DEMO-0025)',          20,  NULL, NULL),
    ('camila.rosero@example.com',      'EMAIL_MARKETING',    'WEB_FORM',  'Casilla de suscripción marcada en formulario web (DEMO-0026)',     20,  NULL, NULL)
) AS x(email, purpose, channel, evidence, days_ago, revoked_days_ago, revocation_reason)
JOIN contacts ct ON ct.email = x.email
JOIN companies co ON co.id = ct.company_id
JOIN processing_purposes p ON p.code = x.purpose;

INSERT INTO products (sku, name, description, unit_price, vat_rate_code) VALUES
    ('CRM-LIC',    'Licencia CRM Pro (usuario/año)',            'Licencia anual por usuario con actualizaciones.',          180.00,  '4'),
    ('IMPL',       'Implementación y configuración',            'Configuración inicial, importación de catálogos y roles.', 1200.00, '4'),
    ('CAP-8H',     'Capacitación presencial (8 horas)',         'Jornada de capacitación para hasta 15 personas.',          350.00,  '4'),
    ('SOP-PREM',   'Soporte premium mensual',                   'Soporte con tiempo de respuesta de 4 horas hábiles.',      250.00,  '4'),
    ('CLOUD-M',    'Servidor en la nube (mes)',                 'Instancia administrada con respaldos diarios.',            95.00,   '4'),
    ('MIG-DAT',    'Migración de datos',                        'Migración desde hojas de cálculo u otro CRM.',            800.00,  '4'),
    ('CONS-LOPDP', 'Consultoría de cumplimiento LOPDP (hora)',  'Acompañamiento para adecuar procesos a la LOPDP.',         60.00,   '4'),
    ('LIB-TEC',    'Manual técnico impreso',                    'Libro impreso (tarifa 0 % de IVA).',                       45.00,   '0');

INSERT INTO opportunities (title, company_id, primary_contact_id, owner_id, stage_code, amount, probability, expected_close_date, closed_at, lost_reason)
SELECT o.title,
       (SELECT id FROM companies WHERE ruc = o.ruc),
       (SELECT id FROM contacts WHERE email = o.contact),
       (SELECT id FROM users WHERE email = o.owner),
       o.stage, o.amount, s.default_probability,
       CURRENT_DATE + o.close_days,
       CASE WHEN s.closed THEN now() + make_interval(days => o.close_days) END,
       o.lost_reason
FROM (VALUES
    ('Plataforma de reservas en línea',         '2099998701001', 'maria.andrade@example.com',     'vendedor1@example.com', 'PROPOSAL',          4384.00,  20,  NULL),
    ('Licencias CRM para operaciones de campo', '2299984128001', 'carolina.vega@example.com',     'vendedor1@example.com', 'NEGOTIATION',       7475.00,  12,  NULL),
    ('Trazabilidad de cosecha',                 '2199976500001', 'valeria.ortiz@example.com',     'vendedor1@example.com', 'QUALIFICATION',     12000.00, 45,  NULL),
    ('ERP para aserradero',                     '1699965185001', 'fernando.lozada@example.com',   'vendedor2@example.com', 'VALUE_PROPOSITION', 18500.00, 60,  NULL),
    ('Tienda en línea de café',                 '1999953472001', 'santiago.jaramillo@example.com','vendedor2@example.com', 'PROSPECTING',       6200.00,  75,  NULL),
    ('Rastreo de flota',                        '1599942811001', 'paola.quishpe@example.com',     'vendedor2@example.com', 'PROPOSAL',          9800.00,  30,  NULL),
    ('Gestión de obras',                        '1499931758001', 'gabriel.tapia@example.com',     'vendedor1@example.com', 'PROSPECTING',       15000.00, 90,  NULL),
    ('Punto de venta ferretero',                '2199920637001', 'andrea.benitez@example.com',    'vendedor2@example.com', 'NEGOTIATION',       5400.00,  8,   NULL),
    ('Campus virtual',                          '1499919545001', 'martin.guerrero@example.com',   'vendedor1@example.com', 'VALUE_PROPOSITION', 22000.00, 50,  NULL),
    ('Asesoría LOPDP y nube',                   '2299981235001', 'ricardo.palacios@example.com',  'vendedor2@example.com', 'CLOSED_WON',        2340.00,  -10, NULL),
    ('Renovación de licencias 2026',            '2099998701001', 'pablo.salazar@example.com',     'vendedor1@example.com', 'CLOSED_WON',        3600.00,  -25, NULL),
    ('Portal de proveedores',                   '2299984128001', 'jorge.espinoza@example.com',    'vendedor1@example.com', 'CLOSED_LOST',       8000.00,  -15, 'El cliente eligió un proveedor local con menor precio.'),
    ('Inventario de bodega',                    '2199920637001', 'andrea.benitez@example.com',    'vendedor2@example.com', 'CLOSED_WON',        4100.00,  -40, NULL),
    ('Capacitación de personal',                '1699965185001', 'daniela.carrion@example.com',   'vendedor2@example.com', 'QUALIFICATION',     1400.00,  20,  NULL),
    ('Integración de pagos',                    '1999953472001', 'camila.rosero@example.com',     'vendedor2@example.com', 'CLOSED_LOST',       3000.00,  -5,  'Proyecto postergado por presupuesto.'),
    ('App de reservas turísticas',              '2099998701001', 'maria.andrade@example.com',     'vendedor1@example.com', 'NEGOTIATION',       9600.00,  35,  NULL)
) AS o(title, ruc, contact, owner, stage, amount, close_days, lost_reason)
JOIN pipeline_stages s ON s.code = o.stage;

INSERT INTO opportunity_stage_history (opportunity_id, from_stage, to_stage, amount, probability, changed_by, changed_at)
SELECT id, NULL, stage_code, amount, probability, owner_id, created_at FROM opportunities;

INSERT INTO activities (type, subject, notes, due_at, remind_at, completed_at, owner_id, company_id, contact_id, opportunity_id)
SELECT a.type, a.subject, a.notes,
       CASE WHEN a.due_hours IS NULL THEN NULL ELSE now() + make_interval(hours => a.due_hours) END,
       CASE WHEN a.remind_hours IS NULL THEN NULL ELSE now() + make_interval(hours => a.remind_hours) END,
       CASE WHEN a.done_hours IS NULL THEN NULL ELSE now() + make_interval(hours => a.done_hours) END,
       o.owner_id, o.company_id,
       (SELECT id FROM contacts WHERE email = a.contact),
       o.id
FROM (VALUES
    ('CALL',    'Llamada de seguimiento de la cotización',  'Confirmar si recibieron la cotización y resolver dudas.', 'Plataforma de reservas en línea',         'maria.andrade@example.com',   26,   24,   NULL),
    ('MEETING', 'Reunión de negociación de licencias',      'Llevar propuesta de descuento por volumen.',              'Licencias CRM para operaciones de campo', 'carolina.vega@example.com',   72,   70,   NULL),
    ('TASK',    'Preparar demo de trazabilidad',            NULL,                                                      'Trazabilidad de cosecha',                 NULL,                          120,  NULL, NULL),
    ('MEETING', 'Visita técnica al aserradero',             'Levantar procesos de inventario.',                       'ERP para aserradero',                     'fernando.lozada@example.com', 48,   46,   NULL),
    ('EMAIL',   'Enviar propuesta de rastreo de flota',     NULL,                                                      'Rastreo de flota',                        'paola.quishpe@example.com',   24,   NULL, NULL),
    ('CALL',    'Confirmar pedido de punto de venta',       'Validar número de cajas y fecha de instalación.',        'Punto de venta ferretero',                'andrea.benitez@example.com',  12,   10,   NULL),
    ('NOTE',    'Cliente satisfecho con la implementación', 'Solicita referencia para otros clientes.',               'Asesoría LOPDP y nube',                   NULL,                          NULL, NULL, -200),
    ('TASK',    'Revisar requisitos del campus virtual',    NULL,                                                      'Campus virtual',                          NULL,                          -24,  NULL, NULL),
    ('CALL',    'Llamada de prospección',                   NULL,                                                      'Gestión de obras',                        'gabriel.tapia@example.com',   168,  NULL, NULL),
    ('MEETING', 'Presentación de valor al rectorado',       'Llevar casos de éxito en educación.',                    'Campus virtual',                          'martin.guerrero@example.com', 96,   94,   NULL),
    ('CALL',    'Primera llamada de descubrimiento',        'Interesados en vender café en línea a Europa.',          'Tienda en línea de café',                 'santiago.jaramillo@example.com', -72, NULL, -72),
    ('TASK',    'Enviar contrato para firma',               NULL,                                                      'App de reservas turísticas',              NULL,                          30,   28,   NULL)
) AS a(type, subject, notes, opportunity, contact, due_hours, remind_hours, done_hours)
JOIN opportunities o ON o.title = a.opportunity;

INSERT INTO quotes (number, opportunity_id, contact_id, status, issue_date, valid_until, subtotal, discount_total, tax_total, total, notes, created_by)
SELECT q.number, o.id, (SELECT id FROM contacts WHERE email = q.contact), q.status,
       CURRENT_DATE - q.issued_days_ago, CURRENT_DATE - q.issued_days_ago + 30,
       q.subtotal, q.discount_total, q.tax_total, q.total, q.notes, o.owner_id
FROM (VALUES
    ('COT-2026-000001', 'Plataforma de reservas en línea',         'maria.andrade@example.com',    'SENT',     3,  4600.00, 216.00, 576.60,  4960.60, 'Incluye 10 % de descuento en licencias por pago anual.'),
    ('COT-2026-000002', 'Licencias CRM para operaciones de campo', 'carolina.vega@example.com',    'SENT',     6,  8300.00, 825.00, 1121.25, 8596.25, 'Descuento por volumen: 15 % en licencias y 5 % en soporte.'),
    ('COT-2026-000003', 'Asesoría LOPDP y nube',                   'ricardo.palacios@example.com', 'ACCEPTED', 20, 2340.00, 0.00,   351.00,  2691.00, NULL)
) AS q(number, opportunity, contact, status, issued_days_ago, subtotal, discount_total, tax_total, total, notes)
JOIN opportunities o ON o.title = q.opportunity;

INSERT INTO quote_items (quote_id, line_number, product_id, description, quantity, unit_price, discount_percent, vat_rate_code, vat_percentage, line_subtotal, line_discount, line_tax, line_total)
SELECT (SELECT id FROM quotes WHERE number = i.number), i.line, p.id, p.name, i.quantity, p.unit_price, i.discount,
       p.vat_rate_code, v.percentage, i.subtotal, i.line_discount, i.tax, i.total
FROM (VALUES
    ('COT-2026-000001', 1, 'CRM-LIC',    12, 10, 2160.00, 216.00, 291.60, 2235.60),
    ('COT-2026-000001', 2, 'IMPL',       1,  0,  1200.00, 0.00,   180.00, 1380.00),
    ('COT-2026-000001', 3, 'CAP-8H',     2,  0,  700.00,  0.00,   105.00, 805.00),
    ('COT-2026-000001', 4, 'LIB-TEC',    12, 0,  540.00,  0.00,   0.00,   540.00),
    ('COT-2026-000002', 1, 'CRM-LIC',    25, 15, 4500.00, 675.00, 573.75, 4398.75),
    ('COT-2026-000002', 2, 'MIG-DAT',    1,  0,  800.00,  0.00,   120.00, 920.00),
    ('COT-2026-000002', 3, 'SOP-PREM',   12, 5,  3000.00, 150.00, 427.50, 3277.50),
    ('COT-2026-000003', 1, 'CONS-LOPDP', 20, 0,  1200.00, 0.00,   180.00, 1380.00),
    ('COT-2026-000003', 2, 'CLOUD-M',    12, 0,  1140.00, 0.00,   171.00, 1311.00)
) AS i(number, line, sku, quantity, discount, subtotal, line_discount, tax, total)
JOIN products p ON p.sku = i.sku
JOIN vat_rates v ON v.code = p.vat_rate_code;

SELECT setval('quote_number_seq', 3);

INSERT INTO data_subject_requests (contact_id, request_type, channel, received_at, due_at, status, completed_at, resolution, handled_by)
SELECT ct.id, 'ACCESS', 'EMAIL', now() - interval '10 days', now() - interval '10 days' + interval '15 days', 'COMPLETED',
       now() - interval '8 days', 'Se entregó al titular la exportación JSON de sus datos por correo.',
       (SELECT id FROM users WHERE email = 'gerente@example.com')
FROM contacts ct WHERE ct.email = 'valeria.ortiz@example.com';
