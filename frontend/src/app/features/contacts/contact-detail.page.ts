import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, saveBlob } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import {
  AUDIT_ACTION_LABELS,
  CHANNEL_LABELS,
  LEGAL_BASIS_LABELS,
  REQUEST_CHANNEL_LABELS,
  SOURCE_LABELS,
  entries,
} from '../../core/labels';
import { AuditLogEntry, Consent, Contact, RequestChannel } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { identificationValidator } from '../../shared/identification';
import { Modal } from '../../shared/modal';
import { applyServerErrors, fieldError } from '../../shared/problem';

@Component({
  selector: 'app-contact-detail',
  imports: [RouterLink, DatePipe, Modal, ReactiveFormsModule, FormsModule],
  template: `
    <div class="page">
      @if (contact(); as c) {
        <div class="page-header">
          <div>
            <a [routerLink]="['/companies', c.companyId]" class="small">← {{ c.companyName }}</a>
            <h1>{{ c.firstName }} {{ c.lastName }}</h1>
            <p>{{ c.jobTitle ?? 'Sin cargo registrado' }} · Titular de datos personales</p>
          </div>
          <button class="btn" type="button" (click)="openEdit(c)">Rectificar datos</button>
        </div>

        <div class="grid-2">
          <section class="card">
            <h2>Datos personales</h2>
            <dl class="details">
              <dt>Correo</dt><dd>{{ c.email ?? '—' }}</dd>
              <dt>Teléfono</dt><dd>{{ c.phone ?? '—' }}</dd>
              <dt>Cédula</dt><dd>{{ c.nationalIdMasked ?? 'No registrada' }}</dd>
              <dt>Origen del dato</dt><dd>{{ sourceLabels[c.dataSource] }}</dd>
              <dt>Registrado</dt><dd>{{ c.createdAt | date: 'dd/MM/yyyy HH:mm' }}</dd>
              <dt>Actualizado</dt><dd>{{ c.updatedAt | date: 'dd/MM/yyyy HH:mm' }}</dd>
            </dl>
            <p class="small muted">Esta consulta queda registrada en el log de accesos a datos personales.</p>
          </section>

          <section class="card">
            <div class="card-header">
              <h2>Consentimientos por finalidad</h2>
              <button class="btn btn-sm btn-primary" type="button" (click)="openGrant()">+ Registrar</button>
            </div>
            <div class="stack">
              @for (consent of c.consents; track consent.id) {
                <div class="consent">
                  <div class="row">
                    <strong>{{ consent.purposeName }}</strong>
                    <span class="spacer"></span>
                    @if (consent.active) {
                      <span class="badge badge-good">✓ Vigente</span>
                    } @else if (consent.revokedAt) {
                      <span class="badge badge-danger">✕ Revocado</span>
                    } @else {
                      <span class="badge badge-warn">⚠ Vencido</span>
                    }
                  </div>
                  <span class="small muted">{{ basis[consent.legalBasis] }} · {{ channelLabels[consent.channel] }} · aviso {{ consent.privacyNoticeVersion }}</span>
                  <span class="small">Evidencia: {{ consent.evidence }}</span>
                  <span class="small muted">
                    Otorgado {{ consent.grantedAt | date: 'dd/MM/yyyy' }} por {{ consent.recordedBy }} · conservar hasta {{ consent.expiresAt | date: 'dd/MM/yyyy' }}
                    @if (consent.revokedAt) { · revocado {{ consent.revokedAt | date: 'dd/MM/yyyy' }}{{ consent.revocationReason ? ': ' + consent.revocationReason : '' }} }
                  </span>
                  @if (consent.active) {
                    <div><button class="btn btn-sm btn-danger" type="button" (click)="revoke(consent)">Revocar</button></div>
                  }
                </div>
              } @empty {
                <p class="muted">Sin registros.</p>
              }
            </div>
          </section>
        </div>

        @if (auth.isManager()) {
          <section class="card stack">
            <h2>Derechos del titular (LOPDP)</h2>
            <p class="muted">
              Acceso y portabilidad (Arts. 13 y 17) y eliminación (Art. 15). Deben atenderse en máximo 15 días; cada solicitud queda en el
              registro de solicitudes (Reglamento Art. 15).
            </p>
            <div class="form-grid">
              <div class="field">
                <label for="dsr-channel">Canal de la solicitud</label>
                <select id="dsr-channel" [(ngModel)]="dsrChannel">
                  @for (ch of requestChannels; track ch.value) { <option [value]="ch.value">{{ ch.label }}</option> }
                </select>
              </div>
              <div class="field">
                <label for="dsr-notes">Nota (sin datos personales)</label>
                <input id="dsr-notes" [(ngModel)]="dsrNotes" maxlength="300" />
              </div>
            </div>
            <div class="row">
              <button class="btn" type="button" (click)="exportData(c)">⬇ Exportar datos (JSON)</button>
              <button class="btn btn-danger" type="button" (click)="erase(c)">Eliminar datos del titular</button>
            </div>
          </section>

          <section class="card">
            <div class="card-header"><h2>Accesos a sus datos</h2><span class="small muted">Últimos 30 días</span></div>
            <div class="table-wrap">
              <table>
                <thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Resultado</th><th>Detalle</th></tr></thead>
                <tbody>
                  @for (entry of accessLog(); track entry.id) {
                    <tr>
                      <td>{{ entry.occurredAt | date: 'dd/MM/yyyy HH:mm' }}</td>
                      <td>{{ entry.userName }}</td>
                      <td>{{ actionLabels[entry.action] }}</td>
                      <td>
                        @if (entry.success) { <span class="badge badge-good">✓ Permitido</span> } @else { <span class="badge badge-danger">✕ Denegado</span> }
                      </td>
                      <td class="small muted">{{ entry.detail ?? '' }}</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="5" class="empty">Sin registros.</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        }
      } @else if (erased()) {
        <div class="card stack">
          <h1>Datos eliminados</h1>
          <p>Los datos del titular fueron eliminados. La solicitud quedó registrada y el log de accesos conserva solo identificadores.</p>
          <a routerLink="/privacy">Ver registro de solicitudes →</a>
        </div>
      } @else {
        <p class="muted">Cargando…</p>
      }
    </div>

    <app-modal [open]="editing()" heading="Rectificar datos del contacto" (closed)="editing.set(false)">
      <form class="form" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="form-grid">
          <div class="field"><label for="e-first">Nombres</label><input id="e-first" formControlName="firstName" />
            @if (err('firstName'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="e-last">Apellidos</label><input id="e-last" formControlName="lastName" />
            @if (err('lastName'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="e-email">Correo</label><input id="e-email" type="email" formControlName="email" />
            @if (err('email'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="e-phone">Teléfono</label><input id="e-phone" formControlName="phone" />
            @if (err('phone'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="e-job">Cargo</label><input id="e-job" formControlName="jobTitle" /></div>
          <div class="field"><label for="e-source">Origen del dato</label>
            <select id="e-source" formControlName="dataSource">
              @for (s of sources; track s.value) { <option [value]="s.value">{{ s.label }}</option> }
            </select></div>
        </div>
        <div class="field">
          <label for="e-id">Nueva cédula</label>
          <input id="e-id" formControlName="nationalId" inputmode="numeric" maxlength="10" autocomplete="off" />
          <span class="hint">Déjela vacía para conservar la actual. Marque la casilla para eliminarla.</span>
          @if (err('nationalId'); as e) { <span class="error">{{ e }}</span> }
          <label class="row small"><input type="checkbox" formControlName="removeNationalId" /> Eliminar la cédula registrada</label>
        </div>
        <div class="form-actions">
          <button class="btn" type="button" (click)="editing.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="submit">Guardar</button>
        </div>
      </form>
    </app-modal>

    <app-modal [open]="granting()" heading="Registrar consentimiento" (closed)="granting.set(false)">
      <form class="form" [formGroup]="consentForm" (ngSubmit)="grant()" novalidate>
        <div class="field">
          <label for="g-purpose">Finalidad</label>
          <select id="g-purpose" formControlName="purposeCode">
            @for (p of catalogs()?.purposes ?? []; track p.code) { <option [value]="p.code">{{ p.name }}</option> }
          </select>
          @if (selectedPurpose(); as p) {
            <span class="hint">{{ p.description }} Base legal: {{ basis[p.legalBasis] }}. Conservación: {{ p.retentionMonths }} meses.</span>
          }
        </div>
        <div class="field">
          <label for="g-channel">Canal</label>
          <select id="g-channel" formControlName="channel">
            @for (ch of channels; track ch.value) { <option [value]="ch.value">{{ ch.label }}</option> }
          </select>
        </div>
        <div class="field">
          <label for="g-evidence">Evidencia verificable</label>
          <input id="g-evidence" formControlName="evidence" maxlength="500" />
          @if (gErr('evidence'); as e) { <span class="error">{{ e }}</span> }
        </div>
        <div class="form-actions">
          <button class="btn" type="button" (click)="granting.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="submit">Registrar</button>
        </div>
      </form>
    </app-modal>
  `,
  styles: `
    .consent { display: grid; gap: 4px; padding: 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); }
  `,
})
export class ContactDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);
  readonly id = input.required<string>();

  protected readonly basis = LEGAL_BASIS_LABELS;
  protected readonly channelLabels = CHANNEL_LABELS;
  protected readonly sourceLabels = SOURCE_LABELS;
  protected readonly actionLabels = AUDIT_ACTION_LABELS;
  protected readonly sources = entries(SOURCE_LABELS);
  protected readonly channels = entries(CHANNEL_LABELS);
  protected readonly requestChannels = entries(REQUEST_CHANNEL_LABELS);
  protected readonly catalogs = toSignal(this.api.catalogs());

  protected readonly contact = signal<Contact | null>(null);
  protected readonly accessLog = signal<AuditLogEntry[]>([]);
  protected readonly erased = signal(false);
  protected readonly editing = signal(false);
  protected readonly granting = signal(false);
  protected dsrChannel: RequestChannel = 'EMAIL';
  protected dsrNotes = '';

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', Validators.email],
    phone: ['', Validators.pattern(/^[0-9+() -]{7,20}$/)],
    jobTitle: [''],
    dataSource: ['DIRECT'],
    nationalId: ['', identificationValidator('CEDULA', true)],
    removeNationalId: [false],
  });

  protected readonly consentForm = this.fb.nonNullable.group({
    purposeCode: ['QUOTE_DELIVERY'],
    channel: ['EMAIL'],
    evidence: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
  });

  private readonly purposeCode = signal('QUOTE_DELIVERY');
  protected readonly selectedPurpose = computed(() => this.catalogs()?.purposes.find((p) => p.code === this.purposeCode()));

  constructor() {
    this.consentForm.controls.purposeCode.valueChanges.subscribe((code) => this.purposeCode.set(code));
  }

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    const id = Number(this.id());
    this.api.contact(id).subscribe((c) => this.contact.set(c));
    if (this.auth.isManager()) {
      this.api.accessLog({ resourceType: 'CONTACT', resourceId: id, size: 50 }).subscribe((p) => this.accessLog.set(p.content));
    }
  }

  protected err(name: string): string | null {
    return fieldError(this.form.get(name));
  }

  protected gErr(name: string): string | null {
    return fieldError(this.consentForm.get(name));
  }

  protected openEdit(c: Contact): void {
    this.form.reset({
      firstName: c.firstName,
      lastName: c.lastName,
      email: c.email ?? '',
      phone: c.phone ?? '',
      jobTitle: c.jobTitle ?? '',
      dataSource: c.dataSource,
      nationalId: '',
      removeNationalId: false,
    });
    this.editing.set(true);
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const blank = (s: string) => (s.trim() ? s.trim() : null);
    this.api
      .updateContact(Number(this.id()), {
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        email: blank(v.email),
        phone: blank(v.phone),
        jobTitle: blank(v.jobTitle),
        dataSource: v.dataSource as never,
        nationalId: v.removeNationalId ? '' : blank(v.nationalId),
      })
      .subscribe({
        next: () => {
          this.editing.set(false);
          this.notifications.success('Datos rectificados.');
          this.load();
        },
        error: (e) => applyServerErrors(this.form, e),
      });
  }

  protected openGrant(): void {
    this.consentForm.reset({ purposeCode: 'QUOTE_DELIVERY', channel: 'EMAIL', evidence: '' });
    this.granting.set(true);
  }

  protected grant(): void {
    this.consentForm.markAllAsTouched();
    if (this.consentForm.invalid) return;
    const v = this.consentForm.getRawValue();
    this.api.grantConsent(Number(this.id()), { purposeCode: v.purposeCode, channel: v.channel as never, evidence: v.evidence.trim() }).subscribe({
      next: () => {
        this.granting.set(false);
        this.notifications.success('Consentimiento registrado.');
        this.load();
      },
      error: (e) => applyServerErrors(this.consentForm, e),
    });
  }

  protected revoke(consent: Consent): void {
    const reason = prompt(`Revocar "${consent.purposeName}". Motivo (opcional; la LOPDP no exige justificación):`, '');
    if (reason === null) return;
    this.api.revokeConsent(Number(this.id()), consent.id, reason).subscribe(() => {
      this.notifications.success('Consentimiento revocado. El registro se conserva como evidencia.');
      this.load();
    });
  }

  protected exportData(c: Contact): void {
    this.api.exportContact(c.id, this.dsrChannel, this.dsrNotes).subscribe((blob) => {
      saveBlob(blob, `contacto-${c.id}-datos.json`);
      this.notifications.success('Exportación generada y registrada como solicitud de acceso/portabilidad.');
      this.load();
    });
  }

  protected erase(c: Contact): void {
    const confirmation = prompt(
      `Esta acción elimina definitivamente los datos de ${c.firstName} ${c.lastName} y anonimiza sus actividades.\nEscriba ELIMINAR para confirmar:`,
    );
    if (confirmation !== 'ELIMINAR') return;
    this.api.eraseContact(c.id, this.dsrChannel, this.dsrNotes).subscribe((result) => {
      this.notifications.success(
        `Datos eliminados: ${result.activitiesAnonymized} actividades anonimizadas, ${result.consentsDeleted} consentimientos borrados.`,
      );
      this.contact.set(null);
      this.erased.set(true);
    });
  }
}
