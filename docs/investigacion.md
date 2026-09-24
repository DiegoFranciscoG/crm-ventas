# Investigación y fuentes

Consulta realizada el **2026-09-24**. Regla aplicada: si dos fuentes se contradicen, gana la oficial y más reciente. Los números `#` se usan como referencia en [modelo-datos.md](modelo-datos.md) y en el código.

## 1. Protección de datos personales (LOPDP, Ecuador)

| # | Fuente (oficial / confiable) | URL | Consultada | Qué se tomó de aquí |
|---|---|---|---|---|
| 1 | Ley Orgánica de Protección de Datos Personales, Quinto Suplemento del Registro Oficial 459, 26-may-2021 (texto publicado por Cancillería) | https://www.cancilleria.gob.ec/wp-content/uploads/2023/03/2021.05.10_ley_organica_de_proteccion_de_datos_personales.pdf | 2026-09-24 | **Art. 7**: 8 bases de legitimación (consentimiento, obligación legal, orden judicial, interés público, medidas precontractuales/contractuales, intereses vitales, bases de acceso público, interés legítimo) → catálogo `legal_basis`. **Art. 8**: consentimiento libre, específico, informado e inequívoco; revocable en cualquier momento sin justificación y sin efecto retroactivo; si hay varias finalidades, debe constar para todas → un registro de consentimiento **por finalidad**. **Art. 10**: principios de finalidad, minimización, conservación (plazos de supresión o revisión periódica), seguridad, responsabilidad proactiva. **Art. 12**: información mínima al titular (fines, base legal, tiempo de conservación, derechos, revocatoria). **Arts. 13, 14, 15 y 16**: acceso, rectificación/actualización, eliminación y oposición, cada uno atendido en **15 días**; la eliminación debe dejar los datos "irreconocibles de forma definitiva y segura". **Art. 17**: portabilidad en formato "estructurado, común, inter-operable y de lectura mecánica" → exportación JSON. **Art. 19**: suspensión del tratamiento. **Art. 37**: medidas como cifrado y seudonimización. **Art. 39**: protección de datos desde el diseño y por defecto. **Art. 43**: notificar vulneraciones a la Autoridad en máximo **5 días (término)**. **Art. 47**: obligaciones del responsable. |
| 2 | Reglamento General a la LOPDP, Decreto Ejecutivo 904, Tercer Suplemento del Registro Oficial 435, 13-nov-2023 (texto publicado por COSEDE) | https://www.cosede.gob.ec/wp-content/uploads/2023/12/REGLAMENTO-GENERAL-A-LA-LEY-ORG%C3%81NICA-DE-PROTECCION-DE-DATOS-PERSONALES_compressed-1.pdf | 2026-09-24 | **Art. 5**: el consentimiento "deberá ser demostrado por el responsable" y "el silencio o la inacción, por sí solos, no presumen el consentimiento" → evidencia obligatoria y ningún consentimiento por defecto. **Art. 6**: tras la revocatoria el responsable debe suspender el tratamiento. **Art. 8**: plazos de conservación no mayores a lo estrictamente necesario. **Art. 9**: al cumplirse la finalidad: eliminación, bloqueo o anonimización. **Art. 10**: el fichero debe registrar obligatoriamente el plazo de conservación → `retention_months` por finalidad. **Art. 15**: "El responsable deberá registrar todas las solicitudes de ejercicio de derechos, incluyendo el detalle de la atención dada" → tabla `data_subject_requests`. **Arts. 38-39**: registro de actividades de tratamiento. |

## 2. Identificación: cédula y RUC

| # | Fuente | URL | Consultada | Qué se tomó de aquí |
|---|---|---|---|---|
| 3 | Comunicado del SRI sobre el cambio en la generación del RUC, citado íntegramente en el registro de defectos #32 de la Plataforma Gob.EC (MINTEL), 18-oct-2021 | https://minka.gob.ec/mintel/ge/rutr/gobec_forms/-/issues/32 | 2026-09-24 | Sociedades (tercer dígito **9**): 13 dígitos; 2 primeros = provincia de la matriz; 6 dígitos secuenciales; el **décimo dígito es verificador con módulo 11 y coeficientes 4,3,2,7,6,5,4,3,2** (dígito = 11 − residuo; residuo 0 → 0); los 3 últimos siempre **001**. **Excepción oficial**: cuando el secuencial pasa de 6 a 7 dígitos, **ya no se aplica el módulo 11** y "no se ha establecido un algoritmo de validación". El SRI recomienda verificar contra su fuente de datos o la consulta pública → el validador **no rechaza** un RUC de sociedad solo por el dígito verificador; lo marca como `checksumVerified=false` y avisa en la interfaz. |
| 4 | SRI, Guía básica de inscripción, actualización y cierre del RUC | https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/ab30edb5-4e52-4623-a550-19f19480f295/Guia+basica+RUC+final.pdf | 2026-09-24 | "En el caso de persona natural es el número de cédula más 001" → RUC de persona natural = cédula válida + `001`. |
| 5 | Registro Civil del Ecuador, "Informativo dígito 30" | https://www.registrocivil.gob.ec/informativo/ | 2026-09-24 | El código inicial **30** se asigna a ecuatorianos nacidos en el exterior que se inscriben en consulados (Resolución 140, Registro Oficial 681, 12-abr-2012) → la cédula admite provincia 01-24 o 30. |
| 6 | INEC, División Político Administrativa (códigos de provincia, cantón y parroquia) | https://www.ecuadorencifras.gob.ec/documentos/web-inec/Poblacion_y_Demografia/CPV_aplicativos/modulo_cpv/dpa_.pdf | 2026-09-24 | Provincias **01 Azuay … 24 Santa Elena** y **90 Zonas no delimitadas** → catálogo `provinces`. El 90 no se usa en cédula ni RUC. |
| 7 | SRI, Ficha técnica de comprobantes electrónicos, esquema offline, v2.32 (actualizada oct-2025) | https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf | 2026-09-24 | **Tabla 6**, tipos de identificación: 04 RUC, 05 cédula, 06 pasaporte, 07 consumidor final, 08 identificación del exterior. **Tabla 16**: código de impuesto IVA = 2. **Tabla 17**, tarifas de IVA: 0 → 0 %, 2 → 12 %, 3 → 14 %, **4 → 15 %**, 5 → 5 %, 6 → no objeto, 7 → exento, 8 → IVA diferenciado, 10 → 13 %. Todo esto alimenta el catálogo `vat_rates`. |

## 3. Tributario (cotizaciones)

| # | Fuente | URL | Consultada | Qué se tomó de aquí |
|---|---|---|---|---|
| 8 | SRI, Circular NAC-DGECCGC25-00000006 (26-dic-2025) | https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=236482f4-6125-42fd-b073-62c99d08233d&nombre=NAC-DGECCGC25-00000006.pdf | 2026-09-24 | El Art. 65 de la LRTI fija el IVA general en 13 % y permite al Presidente modificarlo entre 13 % y 15 %. Si no hay un nuevo decreto, se mantiene la última tarifa: **15 % vigente** (Decreto 470). Como la tarifa puede cambiar por decreto, el IVA es un **dato de catálogo y no una constante en el código**, y cada ítem de cotización guarda la tarifa aplicada. |

## 4. Pipeline y pronóstico de ventas

| # | Fuente | URL | Consultada | Qué se tomó de aquí |
|---|---|---|---|---|
| 9 | Salesforce Help, "Manage Opportunity Stage to Forecast Category Mappings" | https://help.salesforce.com/s/articleView?id=sales.faq_forecasts_category_mapping.htm&language=en_US&type=5 | 2026-09-24 | Mapeo por defecto etapa → probabilidad → categoría: Prospecting 10 % Pipeline, Needs Analysis 20 % Pipeline, Value Proposition 50 % Pipeline, Proposal/Price Quote 75 % Best Case, Negotiation/Review 90 % Commit, Closed Won 100 % Closed, Closed Lost 0 % Omitted. "Si la etapa cambia, cambian la probabilidad y la categoría" → al mover una tarjeta en el kanban se aplica la probabilidad por defecto de la nueva etapa. |
| 10 | HubSpot Knowledge Base, "HubSpot's default deal properties" | https://knowledge.hubspot.com/properties/hubspots-default-deal-properties | 2026-09-24 | **Monto ponderado = monto × probabilidad del negocio**. La fecha de cierre es la fecha prevista o real. Categorías de forecast: Pipeline, Best case, Commit, Closed won → fórmulas del dashboard. |

## 5. Infraestructura gratuita y proveedores

| # | Fuente | URL | Consultada | Qué se tomó de aquí |
|---|---|---|---|---|
| 11 | Render Docs, "Deploy for Free" | https://render.com/docs/free | 2026-09-24 | El servicio web free se suspende tras **15 min** sin tráfico y tarda ~1 min en despertar; **750 h/mes** por workspace; Postgres free expira a los **30 días** (por eso la base va en Neon); **bloquea los puertos 25, 465 y 587**. |
| 12 | Render Changelog (16-sep-2025) | https://render.com/changelog/free-web-services-will-no-longer-allow-outbound-traffic-to-smtp-ports | 2026-09-24 | Confirma el bloqueo de SMTP en el plan free → en producción el email sale por **API HTTPS** (Brevo) y no por SMTP. |
| 13 | Neon Docs, "Plans" | https://neon.com/docs/introduction/plans | 2026-09-24 | Free: **0,5 GB y 100 CU-h por proyecto**, 100 proyectos, **suspensión tras 5 min** sin actividad (no se puede desactivar). |
| 14 | Vercel Docs, "Hobby Plan" (actualizada 14-sep-2026) | https://vercel.com/docs/plans/hobby | 2026-09-24 | Hobby gratis: 100 GB de Fast Data Transfer, 100 despliegues/día, **solo uso personal y no comercial**. Sirve para una demo de portafolio. |
| 15 | Brevo, página de precios | https://www.brevo.com/pricing/ | 2026-09-24 | Plan free: "up to **300 emails per day**" una vez que Brevo aprueba la cuenta para enviar. |
| 16 | Brevo Developers, "Send a transactional email" | https://developers.brevo.com/docs/send-a-transactional-email | 2026-09-24 | `POST https://api.brevo.com/v3/smtp/email`, cabecera `api-key`, campos `sender`, `to`, `subject`, `htmlContent`/`textContent`. El remitente debe estar registrado y verificado. |
| 17 | Resend, "What is Resend pricing" y página de precios | https://resend.com/docs/knowledge-base/what-is-resend-pricing | 2026-09-24 | Alternativa evaluada: 3.000 emails/mes con tope de 100/día. Se descartó como proveedor principal porque exige verificar un dominio propio. |
| 18 | IETF RFC 2606, "Reserved Top Level DNS Names" | https://www.rfc-editor.org/rfc/rfc2606 | 2026-09-24 | `example.com`, `.test`, `.invalid` y `.localhost` están reservados → los datos demo usan esos dominios y el sistema **no envía** correos reales a ellos. |

## 6. Seguridad y stack

| # | Fuente | URL | Consultada | Qué se tomó de aquí |
|---|---|---|---|---|
| 19 | Spring, API oficial de generaciones de Spring Boot | https://api.spring.io/projects/spring-boot/generations | 2026-09-24 | 3.5.x: soporte OSS hasta **2026-06-30** (ya vencido). 4.1.x: soporte OSS hasta **2027-07-31**. Se usa **Spring Boot 4.1.1**, con aprobación explícita del autor. |
| 20 | OWASP Top 10:2025 | https://top10.owasp.org/2025/ | 2026-09-24 | A01 Broken Access Control, A02 Security Misconfiguration, A03 Software Supply Chain Failures, A04 Cryptographic Failures, A05 Injection, A06 Insecure Design, A07 Authentication Failures, A08 Software or Data Integrity Failures, A09 Security Logging and Alerting Failures, A10 Mishandling of Exceptional Conditions. |
| 21 | OWASP API Security Top 10:2023 | https://owasp.org/API-Security/editions/2023/en/0x11-t10/ | 2026-09-24 | API1 BOLA (el vendedor solo ve lo suyo), API3 BOPLA (DTOs explícitos, sin *mass assignment*), API4 consumo de recursos (paginación máxima, rate limiting), API5 BFLA (roles por endpoint). |
| 22 | IETF RFC 7518 (JWA), sección 3.2 | https://www.rfc-editor.org/rfc/rfc7518#section-3.2 | 2026-09-24 | HS256 exige una clave de al menos 256 bits → la app no arranca si `JWT_SECRET` tiene menos de 32 bytes. |
| 23 | OWASP Password Storage Cheat Sheet | https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html | 2026-09-24 | bcrypt con factor de trabajo ≥ 10 y límite de 72 bytes → BCrypt con factor 12 y contraseña de máximo 72 caracteres. |
| 24 | Maven Central, npm registry y Docker Hub | https://repo1.maven.org/maven2/ · https://www.npmjs.com/ · https://hub.docker.com/ | 2026-09-24 | Versiones estables: Spring Boot 4.1.1 (Flyway 12.4, Hibernate 7.4, Testcontainers 2.0.5, Security 7.1, JUnit 6.0), OpenPDF 3.0.5, springdoc 3.1.1, JaCoCo 0.8.15, Angular/CDK 22.2.0, Playwright 1.63.0; imágenes `postgres:17.11-alpine`, `eclipse-temurin:25.0.4_7`, `node:24.21.0-alpine`, `nginx-unprivileged:1.30.5-alpine`, `axllent/mailpit:v1.31.2`. |
| 25 | INEC, Clasificación Nacional de Actividades Económicas CIIU Rev. 4.0 | https://aplicaciones2.ecuadorencifras.gob.ec/SIN/metodologias/CIIU%204.0.pdf | 2026-09-24 | 21 secciones (A-U) → catálogo `industries` para clasificar empresas. |

## 7. Decisiones que salen de la investigación

1. **Consentimiento por finalidad y con evidencia** (#1 Art. 8, #2 Art. 5): cada finalidad (contacto comercial, envío de cotizaciones, marketing por email) tiene su propio registro con fecha, canal, versión del aviso de privacidad y quién lo registró. Revocar no borra el registro: le pone `revoked_at` (sin efecto retroactivo).
2. **Plazo de 15 días** (#1 Arts. 13-16): `due_at = received_at + 15 días calendario`. Ver supuesto S2.
3. **Eliminación segura** (#1 Art. 15, #2 Art. 9): borrado físico del contacto y sus consentimientos. En actividades y cotizaciones (registros comerciales de la empresa) la referencia se desvincula (`contact_id = NULL`) y se anonimizan los textos que lo nombren. El log de accesos guarda solo IDs, así que sobrevive a la eliminación sin conservar datos personales.
4. **Registro de solicitudes** (#2 Art. 15): cada exportación o eliminación crea una fila en `data_subject_requests` con su resultado.
5. **Log de accesos inmutable** (#1 Art. 10 k, responsabilidad proactiva; #20 A09): tabla `access_audit_log` solo de inserción, protegida con un *trigger* que rechaza UPDATE/DELETE.
6. **Cifrado de la cédula del contacto** (#1 Art. 37): es un dato opcional, se guarda cifrado con AES-256-GCM y se muestra enmascarado (`******4567`).
7. **RUC de sociedades sin módulo 11 estricto** (#3): se valida la estructura y el dígito verificador se informa como advertencia.
8. **Email por API HTTPS en producción** (#11, #12, #15, #16) y **SMTP + Mailpit en local**.
9. **Recordatorios con cron externo** (#11): Render free duerme, así que un `@Scheduled` interno no es confiable. GitHub Actions llama cada hora a un endpoint interno protegido con un token.

## Supuestos (no verificados en fuente oficial) — revisar

- **S1. Algoritmo módulo 10 de la cédula**: coeficientes 2,1,2,1,2,1,2,1,2 sobre los 9 primeros dígitos; si un producto es ≥ 10 se le resta 9; verificador = (10 − suma mod 10) mod 10; tercer dígito entre 0 y 5. El Registro Civil **no publica** el algoritmo en una norma. Se usa porque es el estándar de facto (aplicado por plataformas estatales como Gob.EC, #3) y coincide con el formato de RUC natural del SRI (#4). Mitigación: tests con casos calculados a mano y ninguna cédula real.
- **S2. "Plazo de 15 días" como días calendario**: la LOPDP dice "plazo" (no "término"). Se calcula en días calendario, que es la interpretación más favorable al titular (#1 Art. 10 l). Si la Superintendencia de Protección de Datos Personales emite otra directriz, basta cambiar `app.privacy.request-due-days`.
- **S3. RUC de entidades públicas (tercer dígito 6)**: el algoritmo clásico (módulo 11, coeficientes 3,2,7,6,5,4,3,2, verificador en la 9.ª posición, 4 últimos dígitos de establecimiento) no aparece en el comunicado #3. Se aplica igual que para sociedades: estructura obligatoria y verificador como advertencia.
- **S4. Plazos de conservación por finalidad** (12, 24 y 36 meses): la Autoridad aún no los regula (#2 Art. 8). Son valores de negocio configurables en el catálogo `processing_purposes`.
- **S5. IVA 15 %**: vigente según #8 (dic-2025). No se encontró un decreto posterior hasta la fecha de consulta. Está en el catálogo, no en el código.
