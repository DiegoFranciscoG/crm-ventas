import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { APP_CONFIG } from '../config';
import { LoginResponse } from '../models';
import { authInterceptor, errorInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

const API = 'https://api.example.com';

function loginResponse(expiresInMs: number): LoginResponse {
  return {
    accessToken: 'token-de-prueba',
    tokenType: 'Bearer',
    expiresAt: new Date(Date.now() + expiresInMs).toISOString(),
    user: { id: 1, email: 'gerente@example.com', fullName: 'Gerente', role: 'MANAGER', active: true, lastLoginAt: null },
  };
}

describe('AuthService e interceptores', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: { apiUrl: API } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  function login(expiresInMs = 60_000): void {
    auth.login('gerente@example.com', 'x').subscribe();
    httpMock.expectOne(`${API}/api/auth/login`).flush(loginResponse(expiresInMs));
  }

  it('guarda la sesión y expone el rol', () => {
    login();
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.isManager()).toBe(true);
    expect(auth.isAdmin()).toBe(false);
    expect(sessionStorage.getItem('crm.session')).toContain('token-de-prueba');
  });

  it('adjunta el token solo a las peticiones del API propio', () => {
    login();
    http.get(`${API}/api/companies`).subscribe();
    http.get('https://otro-sitio.example.com/api/datos').subscribe();

    expect(httpMock.expectOne(`${API}/api/companies`).request.headers.get('Authorization')).toBe('Bearer token-de-prueba');
    expect(httpMock.expectOne('https://otro-sitio.example.com/api/datos').request.headers.has('Authorization')).toBe(false);
  });

  it('descarta un token vencido', () => {
    login(-1000);
    expect(auth.token()).toBeNull();
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('cierra la sesión ante un 401 del API', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    login();
    http.get(`${API}/api/companies`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${API}/api/companies`).flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { reason: 'expired' } });
  });
});
