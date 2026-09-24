import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { CHANNEL_LABELS, SOURCE_LABELS, TAXPAYER_LABELS, entries } from '../../core/labels';
import { Company, ContactSummary, IdentificationResult, Opportunity } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { identificationValidator } from '../../shared/identification';
import { Modal } from '../../shared/modal';
import { applyServerErrors, fieldError } from '../../shared/problem';
import { buildCompanyForm, companyFormValue, patchCompanyForm, rucFeedback } from './company-form';

@Component({
  selector: 'app-company-detail',
  imports: [RouterLink, Modal, ReactiveFormsModule, CurrencyPipe, DatePipe],
  template: `
    <div class="page">
      @if (company(); as c) {
        <div class="page-header">
          <div>
            <a routerLink="/companies" class="small">← Empresas</a>
            <h1>{{ c.legalName }}</h1>
            <p>
              RUC {{ c.ruc }} · <span class="badge">{{ taxpayer[c.taxpayerType] }}</span>
              @if (!c.rucChecksumVerified) {
                <span class="badge badge-warn" title="El SRI no aplica módulo 11 a RUC con secuencial de 7 dígitos">⚠ Verificador no aplicable</span>
              }
            </p>
          </div>
          <div class="row">
            <button class="btn" type="button" (click)="openEdit(c)">Editar</button>
            @if (auth.isManager()) {
              <button class="btn btn-danger" type="button" (click)="remove(c)">Eliminar</button>
            }
          </div>
        </div>

        <div class="grid-2">
          <section class="card">
            <h2>Datos</h2>
            <dl class="details">
              <dt>Nombre comercial</dt><dd>{{ c.tradeName ?? '—' }}</dd>
              <dt>Actividad CIIU</dt><dd>{{ c.industryName ? c.industryCode + ' · ' + c.industryName : '—' }}</dd>
              <dt>Ubicación</dt><dd>{{ c.city ?? '—' }}, {{ c.provinceName }}</dd>
              <dt>Dirección</dt><dd>{{ c.address ?? '—' }}</dd>
              <dt>Teléfono</dt><dd>{{ c.phone ?? '—' }}</dd>
              <dt>Correo</dt><dd>{{ c.email ?? '—' }}</dd>
              <dt>Sitio web</dt><dd>@if (c.website) { <a [href]="c.website" target="_blank" rel="noopener noreferrer">{{ c.website }}</a> } @else { — }</dd>
              <dt>Dueño de la cuenta</dt><dd>{{ c.owner.fullName }}</dd>
            </dl>
          </section>

          <section class="card">
            <div class="card-header">
              <h2>Contactos</h2>
              <button class="btn btn-sm btn-primary" type="button" (click)="openContact()">+ Contacto</button>
            </div>
            <div class="table-wrap">
              <table>
                <tbody>
                  @for (ct of contacts(); track ct.id) {
                    <tr>
                      <td><a [routerLink]="['/contacts', ct.id]"><strong>{{ ct.fullName }}</strong></a><div class="small muted">{{ ct.jobTitle ?? '' }}</div></td>
                      <td>
                        @if (ct.commercialContactAllowed) {
                          <span class="badge badge-good">✓ Contacto permitido</span>
                        } @else {
                          <span class="badge badge-danger">✕ Sin consentimiento vigente</span>
                        }
                      </td>
                    </tr>
                  } @empty {
                    <tr><td class="empty">Sin contactos registrados.</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <section class="card">
          <h2>Oportunidades</h2>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Título</th><th>Etapa</th><th class="num">Monto</th><th class="num">Prob.</th><th>Cierre</th></tr></thead>
              <tbody>
                @for (o of opportunities(); track o.id) {
                  <tr>
                    <td><a [routerLink]="['/opportunities', o.id]">{{ o.title }}</a></td>
                    <td><span class="badge">{{ o.stageName }}</span></td>
                    <td class="num">{{ o.amount | currency }}</td>
                    <td class="num">{{ o.probability }} %</td>
                    <td>{{ o.expectedCloseDate | date: 'dd/MM/yyyy' }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="empty">Sin oportunidades.</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      } @else {
        <p class="muted">Cargando…</p>
      }
    </div>

    <app-modal [open]="editing()" heading="Editar empresa" (closed)="editing.set(false)">
      <form class="form" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="form-grid">
          <div class="field">
            <label for="c-ruc">RUC</label>
            <input id="c-ruc" formControlName="ruc" inputmode="numeric" maxlength="13" autocomplete="off" />
            @if (err('ruc'); as e) {
              <span class="error">{{ e }}</span>
            } @else if (rucCheck(); as r) {
              <span [class]="r.checksumVerified ? 'ok' : 'warn'">{{ r.checksumVerified ? '✓' : '⚠' }} {{ r.message }}</span>
            }
          </div>
          <div class="field">
            <label for="c-province">Provincia (matriz)</label>
            <select id="c-province" formControlName="provinceCode">
              <option value="">Seleccione…</option>
              @for (p of catalogs()?.provinces ?? []; track p.code) {
                @if (p.code !== '90') { <option [value]="p.code">{{ p.code }} · {{ p.name }}</option> }
              }
            </select>
            @if (err('provinceCode'); as e) { <span class="error">{{ e }}</span> }
          </div>
        </div>
        <div class="field">
          <label for="c-legal">Razón social</label>
          <input id="c-legal" formControlName="legalName" maxlength="300" />
          @if (err('legalName'); as e) { <span class="error">{{ e }}</span> }
        </div>
        <div class="form-grid">
          <div class="field">
            <label for="c-trade">Nombre comercial</label>
            <input id="c-trade" formControlName="tradeName" maxlength="300" />
          </div>
          <div class="field">
            <label for="c-industry">Actividad (CIIU)</label>
            <select id="c-industry" formControlName="industryCode">
              <option value="">Sin clasificar</option>
              @for (i of catalogs()?.industries ?? []; track i.code) {
                <option [value]="i.code">{{ i.code }} · {{ i.name }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="c-city">Ciudad</label>
            <input id="c-city" formControlName="city" maxlength="100" />
          </div>
          <div class="field">
            <label for="c-phone">Teléfono</label>
            <input id="c-phone" formControlName="phone" maxlength="20" />
            @if (err('phone'); as e) { <span class="error">{{ e }}</span> }
          </div>
          <div class="field">
            <label for="c-email">Correo general</label>
            <input id="c-email" type="email" formControlName="email" maxlength="254" />
            @if (err('email'); as e) { <span class="error">{{ e }}</span> }
          </div>
          <div class="field">
            <label for="c-web">Sitio web</label>
            <input id="c-web" formControlName="website" placeholder="https://" maxlength="200" />
            @if (err('website'); as e) { <span class="error">{{ e }}</span> }
          </div>
        </div>
        <div class="field">
          <label for="c-address">Dirección</label>
          <input id="c-address" formControlName="address" maxlength="300" />
        </div>
        @if (serverError()) { <p class="notice notice-danger">{{ serverError() }}</p> }
        <div class="form-actions">
          <button class="btn" type="button" (click)="editing.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="submit">Guardar</button>
        </div>
      </form>
    </app-modal>

    <app-modal [open]="addingContact()" heading="Nuevo contacto" (closed)="addingContact.set(false)">
      <form class="form" [formGroup]="contactForm" (ngSubmit)="saveContact()" novalidate>
        <p class="notice">
          Solo registre los datos necesarios. Para crear el contacto debe documentar su consentimiento para contacto comercial
          (LOPDP Art. 8; el silencio no es consentimiento).
        </p>
        <div class="form-grid">
          <div class="field">
            <label for="ct-first">Nombres</label>
            <input id="ct-first" formControlName="firstName" maxlength="100" />
            @if (cErr('firstName'); as e) { <span class="error">{{ e }}</span> }
          </div>
          <div class="field">
            <label for="ct-last">Apellidos</label>
            <input id="ct-last" formControlName="lastName" maxlength="100" />
            @if (cErr('lastName'); as e) { <span class="error">{{ e }}</span> }
          </div>
          <div class="field">
            <label for="ct-email">Correo</label>
            <input id="ct-email" type="email" formControlName="email" maxlength="254" />
            @if (cErr('email'); as e) { <span class="error">{{ e }}</span> }
          </div>
          <div class="field">
            <label for="ct-phone">Teléfono</label>
            <input id="ct-phone" formControlName="phone" maxlength="20" />
            @if (cErr('phone'); as e) { <span class="error">{{ e }}</span> }
          </div>
          <div class="field">
            <label for="ct-job">Cargo</label>
            <input id="ct-job" formControlName="jobTitle" maxlength="100" />
          </div>
          <div class="field">
            <label for="ct-source">Origen del dato</label>
            <select id="ct-source" formControlName="dataSource">
              @for (s of sources; track s.value) { <option [value]="s.value">{{ s.label }}</option> }
            </select>
          </div>
        </div>
        <div class="field">
          <label for="ct-id">Cédula (opcional)</label>
          <input id="ct-id" formControlName="nationalId" inputmode="numeric" maxlength="10" autocomplete="off" />
          <span class="hint">Solo si es necesaria (p. ej. representante legal). Se guarda cifrada y se muestra enmascarada.</span>
          @if (cErr('nationalId'); as e) { <span class="error">{{ e }}</span> }
        </div>
        <fieldset class="stack" formGroupName="initialConsent">
          <legend><strong>Consentimiento para contacto comercial</strong></legend>
          <div class="field">
            <label for="ct-channel">Canal en que se obtuvo</label>
            <select id="ct-channel" formControlName="channel">
              @for (ch of channels; track ch.value) { <option [value]="ch.value">{{ ch.label }}</option> }
            </select>
          </div>
          <div class="field">
            <label for="ct-evidence">Evidencia verificable</label>
            <input id="ct-evidence" formControlName="evidence" maxlength="500" placeholder="Ej.: formulario web v1.0, registro #1234" />
            @if (cErr('initialConsent.evidence'); as e) { <span class="error">{{ e }}</span> }
          </div>
        </fieldset>
        @if (serverError()) { <p class="notice notice-danger">{{ serverError() }}</p> }
        <div class="form-actions">
          <button class="btn" type="button" (click)="addingContact.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="submit">Registrar contacto</button>
        </div>
      </form>
    </app-modal>
  `,
})
export class CompanyDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);
  readonly id = input.required<string>();

  protected readonly taxpayer = TAXPAYER_LABELS;
  protected readonly sources = entries(SOURCE_LABELS);
  protected readonly channels = entries(CHANNEL_LABELS);
  protected readonly catalogs = toSignal(this.api.catalogs());
  protected readonly company = signal<Company | null>(null);
  protected readonly contacts = signal<ContactSummary[]>([]);
  protected readonly opportunities = signal<Opportunity[]>([]);
  protected readonly editing = signal(false);
  protected readonly addingContact = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly rucCheck = signal<IdentificationResult | null>(null);
  protected readonly form = buildCompanyForm(this.fb);
  protected readonly contactForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', Validators.email],
    phone: ['', Validators.pattern(/^[0-9+() -]{7,20}$/)],
    jobTitle: [''],
    nationalId: ['', identificationValidator('CEDULA', true)],
    dataSource: ['DIRECT'],
    initialConsent: this.fb.nonNullable.group({
      purposeCode: ['COMMERCIAL_CONTACT'],
      channel: ['WEB_FORM'],
      evidence: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
    }),
  });

  constructor() {
    this.form.controls.ruc.valueChanges.subscribe((ruc) => this.rucCheck.set(rucFeedback(ruc)));
  }

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    const id = Number(this.id());
    this.api.company(id).subscribe((c) => this.company.set(c));
    this.api.contacts({ companyId: id, size: 100 }).subscribe((p) => this.contacts.set(p.content));
    this.api.opportunities({ companyId: id, size: 100 }).subscribe((p) => this.opportunities.set(p.content));
  }

  protected err(name: string): string | null {
    return fieldError(this.form.get(name));
  }

  protected cErr(name: string): string | null {
    return fieldError(this.contactForm.get(name));
  }

  protected openEdit(c: Company): void {
    patchCompanyForm(this.form, c);
    this.serverError.set(null);
    this.editing.set(true);
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.api.updateCompany(Number(this.id()), companyFormValue(this.form)).subscribe({
      next: (c) => {
        this.company.set(c);
        this.editing.set(false);
        this.notifications.success('Empresa actualizada.');
      },
      error: (e) => this.serverError.set(applyServerErrors(this.form, e)),
    });
  }

  protected remove(c: Company): void {
    if (!confirm(`¿Eliminar "${c.legalName}"? Solo es posible si no tiene contactos ni oportunidades.`)) return;
    this.api.deleteCompany(c.id).subscribe(() => {
      this.notifications.success('Empresa eliminada.');
      void this.router.navigate(['/companies']);
    });
  }

  protected openContact(): void {
    this.contactForm.reset();
    this.serverError.set(null);
    this.addingContact.set(true);
  }

  protected saveContact(): void {
    this.contactForm.markAllAsTouched();
    if (this.contactForm.invalid) return;
    const v = this.contactForm.getRawValue();
    const blank = (s: string) => (s.trim() ? s.trim() : null);
    this.api
      .createContact({
        companyId: Number(this.id()),
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        email: blank(v.email),
        phone: blank(v.phone),
        jobTitle: blank(v.jobTitle),
        nationalId: blank(v.nationalId),
        dataSource: v.dataSource as never,
        initialConsent: { purposeCode: v.initialConsent.purposeCode, channel: v.initialConsent.channel as never, evidence: v.initialConsent.evidence.trim() },
      })
      .subscribe({
        next: (contact) => {
          this.addingContact.set(false);
          void this.router.navigate(['/contacts', contact.id]);
        },
        error: (e) => this.serverError.set(applyServerErrors(this.contactForm, e)),
      });
  }
}
