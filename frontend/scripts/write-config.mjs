// Escribe public/config.json con la URL del API antes del build (Vercel: variable de entorno API_URL).
// No contiene secretos: la URL del API es pública. Sin API_URL se usa el mismo origen (docker compose / ng serve).
import { writeFileSync } from 'node:fs';

const apiUrl = (process.env.API_URL ?? '').trim().replace(/\/$/, '');
if (apiUrl && !/^https:\/\/[a-z0-9.-]+(:\d+)?$/i.test(apiUrl)) {
  console.error(`API_URL inválida: "${apiUrl}". Use https://dominio sin ruta.`);
  process.exit(1);
}
writeFileSync(new URL('../public/config.json', import.meta.url), `${JSON.stringify({ apiUrl }, null, 2)}\n`);
console.log(`config.json -> apiUrl = "${apiUrl || '(mismo origen)'}"`);
