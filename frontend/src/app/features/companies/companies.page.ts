import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { TAXPAYER_LABELS } from '../../core/labels';
import { CompanySummary, IdentificationResult, Page } from '../../core/models';
import { Modal } from '../../shared/modal';
import { applyServerErrors, fieldError } from '../../shared/problem';
import { buildCompanyForm, companyFormValue, rucFeedback } from './company-form';

@Component({
  selector: 'app-companies',
  imports: [RouterLink, Modal, ReactiveFormsModule, FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Empresas</h1>
          <p>Clientes identificados por RUC. En la lista, el RUC de personas naturales se enmascara porque contiene su cédula.</p>
        </div>
        <button class="btn btn-primary" type="button" (click)="openCreate()">+ Empresa</button>
      </div>
      <div class="card stack">
        <label class="sr-only" for="search">Buscar</label>
        <input id="search" type="search" placeholder="Buscar por razón social, nombre comercial o RUC" [ngModel]="search()" (ngModelChange)="search$.next($event)" />
        <div class="table-wrap">
          <table>
            <thead><tr><th>Razón social</th><th>RUC</th><th>Tipo</th><th>Ubicación</th><th>Dueño</th></tr></thead>
            <tbody>
              @for (c of page()?.content ?? []; track c.id) {
                <tr>
                  <td><a [routerLink]="['/companies', c.id]"><strong>{{ c.legalName }}</strong></a>
                    @if (c.tradeName) { <div class="small muted">{{ c.tradeName }}</div> }
                  </td>
                  <td class="num">{{ c.ruc }}</td>
                  <td><span class="badge">{{ taxpayer[c.taxpayerType] }}</span></td>
                  <td>{{ c.city ?? '—' }}<div class="small muted">{{ c.provinceName }}</div></td>
                  <td>{{ c.owner.fullName }}</td>
                </tr>
              } @empty {
                <tr><td colspan="5" class="empty">No hay empresas que coincidan.</td></tr>
              }
            </tbody>
          </table>
        </div>
        @if (page(); as p) {
          @if (p.totalPages > 1) {
            <div class="row">
              <span class="muted small">{{ p.totalElements }} empresas</span>
              <span class="spacer"></span>
              <button class="btn btn-sm" type="button" [disabled]="p.page === 0" (click)="load(p.page - 1)">Anterior</button>
              <span class="small">Página {{ p.page + 1 }} de {{ p.totalPages }}</span>
              <button class="btn btn-sm" type="button" [disabled]="p.page + 1 >= p.totalPages" (click)="load(p.page + 1)">Siguiente</button>
            </div>
          }
        }
      </div>
    </div>

    <app-modal [open]="creating()" heading="Nueva empresa" (closed)="creating.set(false)">
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
          <button class="btn" type="button" (click)="creating.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="submit" [disabled]="saving()">Guardar</button>
        </div>
      </form>
    </app-modal>
  `,
})
export class CompaniesPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  protected readonly taxpayer = TAXPAYER_LABELS;
  protected readonly catalogs = toSignal(this.api.catalogs());
  protected readonly page = signal<Page<CompanySummary> | null>(null);
  protected readonly search = signal('');
  protected readonly search$ = new Subject<string>();
  protected readonly creating = signal(false);
  protected readonly saving = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly rucCheck = signal<IdentificationResult | null>(null);
  protected readonly form = buildCompanyForm(inject(FormBuilder));

  constructor() {
    this.search$.pipe(debounceTime(300), distinctUntilChanged()).subscribe((term) => {
      this.search.set(term);
      this.load(0);
    });
    this.form.controls.ruc.valueChanges.subscribe((ruc) => this.rucCheck.set(rucFeedback(ruc)));
  }

  ngOnInit(): void {
    this.load(0);
  }

  protected load(page: number): void {
    this.api.companies({ search: this.search(), page, size: 20 }).subscribe((p) => this.page.set(p));
  }

  protected err(name: string): string | null {
    return fieldError(this.form.get(name));
  }

  protected openCreate(): void {
    this.form.reset();
    this.serverError.set(null);
    this.creating.set(true);
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.saving.set(true);
    this.api.createCompany(companyFormValue(this.form)).subscribe({
      next: (company) => {
        this.saving.set(false);
        this.creating.set(false);
        void this.router.navigate(['/companies', company.id]);
      },
      error: (e) => {
        this.saving.set(false);
        this.serverError.set(applyServerErrors(this.form, e));
      },
    });
  }
}
