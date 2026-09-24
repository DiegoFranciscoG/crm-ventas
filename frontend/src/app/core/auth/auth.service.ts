import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { APP_CONFIG } from '../config';
import { LoginResponse, Role, User } from '../models';

interface Session {
  token: string;
  expiresAt: string;
  user: User;
}

const STORAGE_KEY = 'crm.session';

/**
 * Sesión con JWT de corta duración en sessionStorage: se borra al cerrar la pestaña. No se usa una cookie
 * porque frontend (Vercel) y API (Render) están en dominios distintos y serían cookies de terceros.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly config = inject(APP_CONFIG);
  private readonly session = signal<Session | null>(AuthService.restore());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly isManager = computed(() => this.hasRole('MANAGER', 'ADMIN'));
  readonly isAdmin = computed(() => this.hasRole('ADMIN'));

  token(): string | null {
    const current = this.session();
    if (!current) {
      return null;
    }
    if (new Date(current.expiresAt).getTime() <= Date.now()) {
      this.clear();
      return null;
    }
    return current.token;
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.config.apiUrl}/api/auth/login`, { email, password })
      .pipe(
        tap((response) => {
          const session: Session = { token: response.accessToken, expiresAt: response.expiresAt, user: response.user };
          this.session.set(session);
          try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
          } catch {
            // Sin almacenamiento disponible la sesión dura lo que la pestaña.
          }
        }),
      );
  }

  logout(reason?: 'expired'): void {
    this.clear();
    void this.router.navigate(['/login'], { queryParams: reason ? { reason } : {} });
  }

  hasRole(...roles: Role[]): boolean {
    const user = this.session()?.user;
    return !!user && roles.includes(user.role);
  }

  private clear(): void {
    this.session.set(null);
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Nada que limpiar.
    }
  }

  private static restore(): Session | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as Session;
      return new Date(session.expiresAt).getTime() > Date.now() ? session : null;
    } catch {
      return null;
    }
  }
}
