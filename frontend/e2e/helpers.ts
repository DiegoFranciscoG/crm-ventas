import { Page, expect } from '@playwright/test';

/** Contraseña de los usuarios de la demo ficticia (publicada en el README). */
export const DEMO_PASSWORD = process.env['E2E_PASSWORD'] ?? 'Demo-CRM-2026';
export const MANAGER = 'gerente@example.com';
export const REP1 = 'vendedor1@example.com';

export async function login(page: Page, email: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Correo').fill(email);
  await page.getByLabel('Contraseña').fill(DEMO_PASSWORD);
  await page.getByRole('button', { name: 'Ingresar' }).click();
  await expect(page.getByRole('heading', { name: 'Pipeline', level: 1 })).toBeVisible();
}

/** RUC sintético de sociedad (provincia 24, secuencial alto) con verificador módulo 11 válido. */
export function uniqueRuc(): string {
  const coefficients = [4, 3, 2, 7, 6, 5, 4, 3, 2];
  for (;;) {
    const base = `249${String(900000 + Math.floor(Math.random() * 99999)).padStart(6, '0')}`;
    const sum = coefficients.reduce((acc, c, i) => acc + Number(base[i]) * c, 0);
    const remainder = sum % 11;
    const check = remainder === 0 ? 0 : 11 - remainder;
    if (check < 10) {
      return `${base}${check}001`;
    }
  }
}
