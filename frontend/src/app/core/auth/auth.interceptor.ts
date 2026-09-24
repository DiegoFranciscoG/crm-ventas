import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { APP_CONFIG } from '../config';
import { NotificationService } from '../notification.service';
import { ProblemDetail } from '../models';
import { AuthService } from './auth.service';

/** Adjunta el token solo a las peticiones hacia el API propio (nunca a terceros). */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const config = inject(APP_CONFIG);
  const token = auth.token();
  const isApi = request.url.startsWith(`${config.apiUrl}/api/`);
  const authorized = token && isApi ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(authorized);
};

/** Traduce ProblemDetail a mensajes; un 401 fuera del login cierra la sesión. */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const notifications = inject(NotificationService);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const problem = (error.error ?? {}) as ProblemDetail;
        const isLogin = request.url.endsWith('/api/auth/login');
        if (error.status === 401 && !isLogin) {
          auth.logout('expired');
        } else if (error.status === 0) {
          notifications.error('No se pudo conectar con el servidor. Si es la demo gratuita, puede tardar un minuto en despertar.');
        } else if (error.status !== 400 && error.status !== 401) {
          notifications.error(problem.detail ?? 'Ocurrió un error inesperado.');
        }
      }
      return throwError(() => error);
    }),
  );
};
