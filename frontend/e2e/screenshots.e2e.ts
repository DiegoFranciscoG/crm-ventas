import { test } from '@playwright/test';
import { MANAGER, REP1, login } from './helpers';

/**
 * Genera las capturas del README en docs/img. Solo se ejecuta con SCREENSHOTS=1:
 *   SCREENSHOTS=1 npx playwright test screenshots
 */
test.skip(!process.env['SCREENSHOTS'], 'Solo para regenerar capturas del README');

const OUT = '../docs/img';

test('capturas del README', async ({ page }) => {
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
  await page.getByRole('link', { name: 'Turismo Insular Galápagos Cía. Ltda.' }).click();
  await page.getByRole('link', { name: 'María José Andrade' }).click();
  await page.getByText('Consentimientos por finalidad').waitFor();
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${OUT}/contacto-lopdp.png`, fullPage: true });

  await page.goto('/quotes/1');
  await page.getByRole('heading', { name: 'COT-2026-000001' }).waitFor();
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${OUT}/cotizacion.png`, fullPage: true });

  await page.getByRole('link', { name: 'Privacidad LOPDP' }).click();
  await page.getByRole('tab', { name: 'Log de accesos' }).click();
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${OUT}/log-accesos.png` });

  await page.emulateMedia({ colorScheme: 'dark' });
  await page.getByRole('link', { name: 'Pipeline' }).first().click();
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${OUT}/pipeline-oscuro.png` });
});
