# Política de seguridad

## Reportar una vulnerabilidad
No abras un issue público. Escríbeme por GitHub (perfil DiegoFranciscoG) con los pasos para reproducir el problema. Respondo en un máximo de 7 días.

Si la vulnerabilidad expone datos personales, indícalo: la LOPDP (Art. 43) exige notificar a la Autoridad en un término de 5 días desde que se tiene constancia de una vulneración.

## Prácticas aplicadas en este proyecto
- Secretos solo por variables de entorno (`.env` no se versiona; ver `.env.example`). La aplicación no arranca si faltan o son débiles (JWT ≥ 256 bits, clave AES de 32 bytes, CORS sin `*`).
- Escaneo de secretos con gitleaks en cada push y pull request; acciones de GitHub fijadas por SHA con permisos mínimos; Dependabot para Maven, npm, Docker y Actions.
- Autenticación JWT de corta duración, BCrypt (factor 12) y rate limiting de login por cuenta y por IP.
- Autorización deny-by-default, por rol (funciones) y por dueño (objetos). Los accesos ajenos responden 404 y se registran.
- Validación de entradas con Bean Validation; errores sin trazas (ProblemDetail); solo `/actuator/health` expuesto.
- Datos personales: consentimiento por finalidad con evidencia, cédula cifrada con AES-256-GCM y enmascarada, log de accesos inmutable y derechos de acceso/eliminación con registro de solicitudes.
- Contenedores multi-stage con versión fija, usuario no root y healthcheck; CSP estricta y cabeceras de seguridad en API y frontend.

## Alcance
La demo pública usa solo datos ficticios. No ingreses datos personales reales en ella.
