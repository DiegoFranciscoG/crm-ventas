import { expect, test } from '@playwright/test';
import { MANAGER, REP1, login, uniqueRuc } from './helpers';

test.describe('CRM Ventas - flujos críticos', () => {
  test('el gerente ve el pipeline de todo el equipo y el forecast', async ({ page }) => {
    await login(page, MANAGER);
    await expect(page.locator('section.column')).toHaveCount(7);
    await expect(page.getByLabel('Vendedor')).toBeVisible();

    await page.getByRole('link', { name: 'Forecast' }).click();
    await expect(page.getByText('Pipeline ponderado')).toBeVisible();
    await page.getByRole('button', { name: 'Ver tabla' }).click();
    await expect(page.getByRole('columnheader', { name: 'Ponderado' }).first()).toBeVisible();
  });

  test('el vendedor solo ve lo suyo y no accede a privacidad', async ({ page }) => {
    await login(page, REP1);
    await expect(page.getByLabel('Vendedor')).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Privacidad LOPDP' })).toHaveCount(0);

    await page.goto('/privacy');
    await expect(page).toHaveURL(/\/pipeline$/);

    await page.getByRole('link', { name: 'Empresas' }).click();
    await expect(page.locator('tbody tr td:nth-child(5)').first()).toBeVisible();
    const owners = await page.locator('tbody tr td:nth-child(5)').allTextContents();
    expect(owners.length).toBeGreaterThan(0);
    expect(new Set(owners.map((o) => o.trim()))).toEqual(new Set(['Andrés Villacís']));
  });

  test('el alta de empresa valida el RUC con las reglas del SRI', async ({ page }) => {
    await login(page, REP1);
    await page.getByRole('link', { name: 'Empresas' }).click();
    await page.getByRole('button', { name: '+ Empresa' }).click();

    const ruc = page.getByLabel('RUC');
    await ruc.fill('2599998701001');
    await ruc.blur();
    await expect(page.getByText('Código de provincia inválido')).toBeVisible();

    await ruc.fill('2299981235001');
    await expect(page.getByText('El SRI no aplica módulo 11')).toBeVisible();

    await ruc.fill(uniqueRuc());
    await expect(page.getByText('RUC de sociedad válido.')).toBeVisible();
    await page.getByLabel('Provincia (matriz)').selectOption('24');
    await page.getByLabel('Razón social').fill('Empresa E2E S.A.');
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('heading', { name: 'Empresa E2E S.A.' })).toBeVisible();
  });

  test('arrastrar una oportunidad en el kanban actualiza su probabilidad', async ({ page }) => {
    await login(page, REP1);
    const title = `Kanban E2E ${Date.now()}`;
    await page.getByRole('button', { name: '+ Oportunidad' }).click();
    await page.getByLabel('Título').fill(title);
    await page.getByLabel('Empresa').selectOption({ index: 1 });
    await page.getByLabel('Monto (USD, sin IVA)').fill('4000');
    await page.getByRole('button', { name: 'Crear' }).click();
    await expect(page.getByRole('heading', { name: title })).toBeVisible();

    await page.getByRole('link', { name: 'Pipeline' }).first().click();
    const card = page.locator('section[aria-label="Prospección"] .opp', { hasText: title });
    const target = page.locator('section[aria-label="Calificación"] .cards');
    await card.scrollIntoViewIfNeeded();
    const from = (await card.boundingBox())!;
    const to = (await target.boundingBox())!;
    await page.mouse.move(from.x + from.width / 2, from.y + from.height - 12);
    await page.mouse.down();
    await page.mouse.move(from.x + from.width / 2 + 10, from.y + from.height - 5, { steps: 5 });
    await page.mouse.move(to.x + to.width / 2, to.y + 30, { steps: 25 });
    await page.mouse.up();

    await expect(page.getByText(`"${title}" pasó a Calificación (20 %).`)).toBeVisible();
    await expect(page.locator('section[aria-label="Calificación"] .opp', { hasText: title })).toBeVisible();
  });

  test('la cotización calcula el IVA y se descarga en PDF', async ({ page }) => {
    await login(page, REP1);
    await page.locator('section[aria-label="Negociación"] .opp a.title').first().click();
    await page.getByRole('link', { name: '+ Cotización' }).click();
    await expect(page.getByRole('heading', { name: 'Nueva cotización' })).toBeVisible();

    await page.getByLabel('Producto de la línea 1').selectOption({ label: 'IMPL · Implementación y configuración' });
    await page.getByLabel('Cantidad línea 1').fill('2');
    await expect(page.locator('.totals .grand')).toContainText('2.760,00');
    await page.getByRole('button', { name: 'Crear cotización' }).click();
    await expect(page.getByRole('heading', { name: /COT-\d{4}-\d{6}/ })).toBeVisible();

    const download = page.waitForEvent('download');
    await page.getByRole('button', { name: '⬇ PDF' }).click();
    expect((await download).suggestedFilename()).toMatch(/^COT-\d{4}-\d{6}\.pdf$/);
  });
});
