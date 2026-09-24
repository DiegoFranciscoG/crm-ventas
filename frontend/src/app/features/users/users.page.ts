import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ROLE_LABELS, entries } from '../../core/labels';
import { Role, User } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Modal } from '../../shared/modal';
import { applyServerErrors, fieldError } from '../../shared/problem';

@Component({
  selector: 'app-users',
  imports: [DatePipe, Modal, ReactiveFormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Usuarios</h1>
          <p>Los usuarios no se eliminan: se desactivan, porque el log de accesos los referencia.</p>
        </div>
        @if (auth.isAdmin()) { <button class="btn btn-primary" type="button" (click)="open(null)">+ Usuario</button> }
      </div>
      <div class="card table-wrap">
        <table>
          <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Último ingreso</th>@if (auth.isAdmin()) { <th></th> }</tr></thead>
          <tbody>
            @for (u of users(); track u.id) {
              <tr>
                <td><strong>{{ u.fullName }}</strong></td>
                <td>{{ u.email }}</td>
                <td>{{ roleLabels[u.role] }}</td>
                <td>@if (u.active) { <span class="badge badge-good">✓ Activo</span> } @else { <span class="badge">Desactivado</span> }</td>
                <td>{{ u.lastLoginAt ? (u.lastLoginAt | date: 'dd/MM/yyyy HH:mm') : '—' }}</td>
                @if (auth.isAdmin()) { <td><button class="btn btn-sm" type="button" (click)="open(u)">Editar</button></td> }
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    <app-modal [open]="editing()" [heading]="current() ? 'Editar usuario' : 'Nuevo usuario'" (closed)="editing.set(false)">
      <form class="form" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="form-grid">
          <div class="field"><label for="u-name">Nombre completo</label><input id="u-name" formControlName="fullName" maxlength="150" />
            @if (err('fullName'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="u-email">Correo</label><input id="u-email" type="email" formControlName="email" [readonly]="!!current()" />
            @if (err('email'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="u-role">Rol</label>
            <select id="u-role" formControlName="role">
              @for (r of roles; track r.value) { <option [value]="r.value">{{ r.label }}</option> }
            </select></div>
          <div class="field"><label for="u-pass">{{ current() ? 'Nueva contraseña (opcional)' : 'Contraseña' }}</label>
            <input id="u-pass" type="password" formControlName="password" autocomplete="new-password" />
            <span class="hint">Entre 12 y 72 caracteres.</span>
            @if (err('password'); as e) { <span class="error">{{ e }}</span> }</div>
        </div>
        @if (current()) { <label class="row"><input type="checkbox" formControlName="active" /> Usuario activo</label> }
        <div class="form-actions">
          <button class="btn" type="button" (click)="editing.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="submit">Guardar</button>
        </div>
      </form>
    </app-modal>
  `,
})
export class UsersPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roles = entries(ROLE_LABELS);
  protected readonly users = signal<User[]>([]);
  protected readonly editing = signal(false);
  protected readonly current = signal<User | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email]],
    role: ['SALES_REP' as Role],
    password: ['', [Validators.minLength(12), Validators.maxLength(72)]],
    active: [true],
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.api.users().subscribe((u) => this.users.set(u));
  }

  protected err(name: string): string | null {
    return fieldError(this.form.get(name));
  }

  protected open(user: User | null): void {
    this.current.set(user);
    this.form.reset(user ? { fullName: user.fullName, email: user.email, role: user.role, password: '', active: user.active } : undefined);
    const password = this.form.controls.password;
    password.setValidators(user ? [Validators.minLength(12), Validators.maxLength(72)] : [Validators.required, Validators.minLength(12), Validators.maxLength(72)]);
    password.updateValueAndValidity();
    this.editing.set(true);
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const user = this.current();
    const request = user
      ? this.api.updateUser(user.id, { fullName: v.fullName.trim(), role: v.role, active: v.active, newPassword: v.password || null })
      : this.api.createUser({ email: v.email.trim(), fullName: v.fullName.trim(), password: v.password, role: v.role });
    request.subscribe({
      next: () => {
        this.editing.set(false);
        this.notifications.success('Usuario guardado.');
        this.load();
      },
      error: (e) => applyServerErrors(this.form, e),
    });
  }
}
