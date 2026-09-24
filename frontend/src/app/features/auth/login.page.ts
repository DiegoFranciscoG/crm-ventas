import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { fieldError } from '../../shared/problem';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  styles: `
    .login { display: grid; place-items: center; min-height: 100vh; padding: 16px; }
    .panel { width: min(420px, 100%); display: grid; gap: 20px; }
    .brand { display: flex; align-items: center; gap: 12px; }
    .logo { display: grid; place-items: center; width: 44px; height: 44px; border-radius: 10px; background: var(--accent); color: var(--on-accent); font-size: 1.3rem; }
  `,
  template: `
    <div class="login">
      <div class="panel">
        <div class="brand">
          <span class="logo" aria-hidden="true">◆</span>
          <div>
            <h1>CRM Ventas</h1>
            <span class="muted">Pipeline B2B con cumplimiento LOPDP</span>
          </div>
        </div>
        <form class="card form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
          @if (reason() === 'expired') {
            <p class="notice notice-warn">Su sesión terminó. Vuelva a ingresar.</p>
          }
          <div class="field">
            <label for="email">Correo</label>
            <input id="email" type="email" formControlName="email" autocomplete="username" />
            @if (err('email'); as e) { <span class="error">{{ e }}</span> }
          </div>
          <div class="field">
            <label for="password">Contraseña</label>
            <input id="password" type="password" formControlName="password" autocomplete="current-password" />
            @if (err('password'); as e) { <span class="error">{{ e }}</span> }
          </div>
          @if (error()) { <p class="notice notice-danger" role="alert">{{ error() }}</p> }
          <button class="btn btn-primary" type="submit" [disabled]="loading()">
            {{ loading() ? 'Ingresando…' : 'Ingresar' }}
          </button>
          <p class="small muted">
            Demo: gerente&#64;example.com (ve todo) o vendedor1&#64;example.com (ve lo suyo). La contraseña está en el README.
          </p>
        </form>
      </div>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly returnUrl = input<string>();
  readonly reason = input<string>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.maxLength(72)]],
  });

  protected err(name: 'email' | 'password'): string | null {
    return fieldError(this.form.controls[name]);
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => void this.router.navigateByUrl(this.returnUrl() || '/pipeline'),
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(
          e.status === 429 ? (e.error?.detail ?? 'Demasiados intentos.') : e.status === 0 ? 'Servidor no disponible.' : 'Correo o contraseña incorrectos.',
        );
      },
    });
  }
}
