import { test } from '@playwright/test';
import { MANAGER, REP1, login } from './helpers';

/**
 * Genera las capturas del README en docs/img. Solo se ejecuta con SCREENSHOTS=1, sobre datos demo recién cargados:
 *   SCREENSHOTS=1 E2E_API_URL=http://localhost:8080 npx playwright test screenshots
 * El PDF de la cotización se guarda en test-results/ (no se versiona) para convertirlo a imagen.
 */
test.skip(!process.env['SCREENSHOTS'], 'Solo para regenerar capturas del README');

const OUT = '../docs/img';
const API = process.env['E2E_API_URL'] ?? 'http://localhost:8080';

test('capturas del README', async ({ page }) => {
  test.setTimeout(120_000);
  await page.emulateMedia({ colorScheme: 'light' });
  // Un vendedor intenta abrir un contacto de otra vendedora (ID 6 en los datos demo): queda como acceso denegado.
  await login(page, REP1);
  await page.goto('/contacts/6');
  await page.getByText('Contacto no encontrado.').waitFor();

  await login(page, MANAGER);
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${OUT}/pipeline.png` });

  await page.getByRole('link', { name: 'Forecast' }).click();
  await page.getByText('Pipeline ponderado').waitFor();
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${OUT}/forecast.png`, fullPage: true });

  await page.getByRole('link', { name: 'Empresas' }).click();
  await page.getByRole('button', { name: '+ Empresa' }).click();
  await page.getByLabel('RUC').fill('2299981235001');
  await page.getByLabel('Provincia (matriz)').selectOption('22');
  await page.getByLabel('Razón social').fill('Nueva Empresa Amazónica S.A.');
  await page.getByText('El SRI no aplica módulo 11').waitFor();
  await page.waitForTimeout(300);
  await page.screenshot({ path: `${OUT}/alta-empresa-ruc.png` });
  await page.getByRole('button', { name: 'Cancelar' }).click();

  await page.getByRole('link', { name: 'Turismo Insular Galápagos Cía. Ltda.' }).click();
  await page.getByRole('button', { name: '+ Contacto' }).click();
  await page.getByLabel('Nombres').fill('Carla');
  await page.getByLabel('Cédula (opcional)').fill('2059999992');
  await page.getByLabel('Cédula (opcional)').blur();
  await page.getByText('El dígito verificador de la cédula no es correcto.').waitFor();
  await page.waitForTimeout(300);
  await page.screenshot({ path: `${OUT}/alta-contacto-consentimiento.png` });
  await page.getByRole('button', { name: 'Cancelar' }).click();

  await page.getByRole('link', { name: 'María José Andrade' }).click();
  await page.getByText('Consentimientos por finalidad').waitFor();
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${OUT}/contacto-lopdp.png`, fullPage: true });

  await page.goto('/quotes/1');
  await page.getByRole('heading', { name: 'COT-2026-000001' }).waitFor();
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${OUT}/cotizacion.png`, fullPage: true });
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: '⬇ PDF' }).click();
  await (await download).saveAs('test-results/cotizacion.pdf');

  await page.getByRole('link', { name: 'Privacidad LOPDP' }).click();
  await page.getByRole('tab', { name: 'Log de accesos' }).click();
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${OUT}/log-accesos.png` });

  await page.emulateMedia({ colorScheme: 'dark' });
  await page.getByRole('link', { name: 'Pipeline' }).first().click();
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${OUT}/pipeline-oscuro.png` });

  await page.emulateMedia({ colorScheme: 'light' });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/activities');
  await page.getByRole('heading', { name: 'Agenda' }).waitFor();
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${OUT}/movil-agenda.png` });

  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto(`${API}/swagger-ui.html`);
  await page.getByText('CRM Ventas API').first().waitFor();
  await page.waitForTimeout(800);
  await page.screenshot({ path: `${OUT}/swagger.png` });
});
