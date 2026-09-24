import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeEsEc from '@angular/common/locales/es-EC';
import { ApplicationConfig, DEFAULT_CURRENCY_CODE, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor, errorInterceptor } from './core/auth/auth.interceptor';
import { APP_CONFIG, AppConfig } from './core/config';

registerLocaleData(localeEsEc);

export function buildAppConfig(config: AppConfig): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes, withComponentInputBinding()),
      provideHttpClient(withFetch(), withInterceptors([authInterceptor, errorInterceptor])),
      { provide: APP_CONFIG, useValue: config },
      { provide: LOCALE_ID, useValue: 'es-EC' },
      { provide: DEFAULT_CURRENCY_CODE, useValue: 'USD' },
    ],
  };
}
