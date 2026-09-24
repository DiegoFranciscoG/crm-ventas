import { InjectionToken } from '@angular/core';

/** Configuración de ejecución: se lee de /config.json para que un mismo build sirva en Docker y en Vercel. */
export interface AppConfig {
  /** URL base del API. Vacía = mismo origen (proxy de nginx o del servidor de desarrollo). */
  apiUrl: string;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');

export async function loadConfig(): Promise<AppConfig> {
  try {
    const response = await fetch('/config.json', { cache: 'no-store' });
    if (response.ok) {
      const config = (await response.json()) as Partial<AppConfig>;
      return { apiUrl: (config.apiUrl ?? '').replace(/\/$/, '') };
    }
  } catch {
    // Sin config.json se usa el mismo origen.
  }
  return { apiUrl: '' };
}
