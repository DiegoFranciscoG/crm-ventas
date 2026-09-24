import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { QUOTE_STATUS_LABELS } from '../../core/labels';
import { Activity, ContactSummary, Opportunity, QuoteSummary, StageHistory, User } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { ActivityForm } from '../activities/activity-form';
import { Modal } from '../../shared/modal';
import { applyServerErrors, fieldError } from '../../shared/problem';

@Component({
  selector: 'app-opportunity-detail',
  imports: [RouterLink, CurrencyPipe, DatePipe, Modal, ReactiveFormsModule, ActivityForm],
  template: `
    <div class="page">
      @if (opportunity(); as o) {
        <div class="page-header">
          <div>
            <a routerLink="/pipeline" class="small">← Pipeline</a>
            <h1>{{ o.title }}</h1>
            <p><a [routerLink]="['/companies', o.companyId]">{{ o.companyName }}</a> · <span class="badge badge-accent">{{ o.stageName }}</span></p>
          </div>
          <div class="row">
            <button class="btn" type="button" (click)="openEdit(o)">Editar</button>
            @if (!stageClosed()) {
              <a class="btn btn-primary" [routerLink]="['/quotes/new']" [queryParams]="{ opportunityId: o.id }">+ Cotización</a>
            }
            @if (auth.isManager()) {
              <button class="btn btn-danger" type="button" (click)="remove(o)">Eliminar</button>
            }
          </div>
        </div>

        <div class="grid-2">
          <section class="card">
            <h2>Resumen</h2>
            <dl class="details">
              <dt>Monto</dt><dd>{{ o.amount | currency }}</dd>
              <dt>Probabilidad</dt><dd>{{ o.probability }} %</dd>
              <dt>Monto ponderado</dt><dd>{{ o.weightedAmount | currency }}</dd>
              <dt>Cierre estimado</dt><dd>{{ o.expectedCloseDate | date: 'dd/MM/yyyy' }}</dd>
              @if (o.closedAt) { <dt>Cerrada</dt><dd>{{ o.closedAt | date: 'dd/MM/yyyy HH:mm' }}</dd> }
              @if (o.lostReason) { <dt>Motivo de pérdida</dt><dd>{{ o.lostReason }}</dd> }
              <dt>Contacto principal</dt>
              <dd>@if (o.primaryContactId) { <a [routerLink]="['/contacts', o.primaryContactId]">{{ o.primaryContactName }}</a> } @else { — }</dd>
              <dt>Dueño</dt><dd>{{ o.owner.fullName }}</dd>
              <dt>Descripción</dt><dd>{{ o.description ?? '—' }}</dd>
            </dl>
          </section>
          <section class="card">
            <h2>Historial de etapas</h2>
            <ol class="timeline">
              @for (h of history(); track $index) {
                <li>
                  <strong>{{ stageName(h.toStage) }}</strong> · {{ h.probability }} %
                  <div class="small muted">{{ h.changedAt | date: 'dd/MM/yyyy HH:mm' }} · {{ h.changedBy }}</div>
                </li>
              }
            </ol>
          </section>
        </div>

        <section class="card">
          <div class="card-header"><h2>Cotizaciones</h2></div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Número</th><th>Estado</th><th>Emisión</th><th>Válida hasta</th><th class="num">Total</th></tr></thead>
              <tbody>
                @for (q of quotes(); track q.id) {
                  <tr>
                    <td><a [routerLink]="['/quotes', q.id]">{{ q.number }}</a></td>
                    <td><span class="badge">{{ quoteStatus[q.status] }}</span></td>
                    <td>{{ q.issueDate | date: 'dd/MM/yyyy' }}</td>
                    <td>{{ q.validUntil | date: 'dd/MM/yyyy' }}</td>
                    <td class="num">{{ q.total | currency }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="empty">Sin cotizaciones.</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>

        <section class="card stack">
          <div class="card-header"><h2>Actividades</h2></div>
          <app-activity-form [companyId]="o.companyId" [opportunityId]="o.id" [contacts]="contacts()" (saved)="loadActivities()" />
          <div class="table-wrap">
            <table>
              <tbody>
                @for (a of activities(); track a.id) {
                  <tr>
                    <td><input type="checkbox" [checked]="!!a.completedAt" (change)="toggle(a)" [attr.aria-label]="'Completar ' + a.subject" /></td>
                    <td [class.muted]="a.completedAt"><strong>{{ a.subject }}</strong>
                      @if (a.contactName) { <div class="small muted">con {{ a.contactName }}</div> }
                    </td>
                    <td>{{ a.dueAt ? (a.dueAt | date: 'dd/MM/yyyy HH:mm') : '—' }}</td>
                    <td>
                      @if (a.overdue) { <span class="badge badge-warn">⚠ Atrasada</span> }
                      @if (a.remindAt && !a.reminderSentAt && !a.completedAt) { <span class="badge">🔔 {{ a.remindAt | date: 'dd/MM HH:mm' }}</span> }
                    </td>
                  </tr>
                } @empty {
                  <tr><td class="empty">Sin actividades.</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      } @else {
        <p class="muted">Cargando…</p>
      }
    </div>

    <app-modal [open]="editing()" heading="Editar oportunidad" (closed)="editing.set(false)">
      <form class="form" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="field"><label for="o-title">Título</label><input id="o-title" formControlName="title" maxlength="200" />
          @if (err('title'); as e) { <span class="error">{{ e }}</span> }</div>
        <div class="form-grid">
          <div class="field"><label for="o-amount">Monto (USD)</label><input id="o-amount" type="number" min="0" step="0.01" formControlName="amount" />
            @if (err('amount'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="o-date">Cierre estimado</label><input id="o-date" type="date" formControlName="expectedCloseDate" /></div>
          <div class="field"><label for="o-prob">Probabilidad (%)</label><input id="o-prob" type="number" min="0" max="100" formControlName="probability" />
            @if (err('probability'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="o-contact">Contacto principal</label>
            <select id="o-contact" formControlName="primaryContactId">
              <option [ngValue]="null">Ninguno</option>
              @for (c of contacts(); track c.id) { <option [ngValue]="c.id">{{ c.fullName }}</option> }
            </select></div>
          @if (auth.isManager()) {
            <div class="field"><label for="o-owner">Dueño</label>
              <select id="o-owner" formControlName="ownerId">
                @for (u of users(); track u.id) { <option [ngValue]="u.id">{{ u.fullName }}</option> }
              </select></div>
          }
        </div>
        <div class="field"><label for="o-desc">Descripción</label><textarea id="o-desc" formControlName="description" maxlength="2000"></textarea></div>
        <div class="form-actions">
          <button class="btn" type="button" (click)="editing.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="submit">Guardar</button>
        </div>
      </form>
    </app-modal>
  `,
  styles: `
    .timeline { margin: 0; padding-left: 18px; display: grid; gap: 8px; }
  `,
})
export class OpportunityDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);
  readonly id = input.required<string>();

  protected readonly quoteStatus = QUOTE_STATUS_LABELS;
  private readonly catalogs = toSignal(this.api.catalogs());
  protected readonly opportunity = signal<Opportunity | null>(null);
  protected readonly history = signal<StageHistory[]>([]);
  protected readonly quotes = signal<QuoteSummary[]>([]);
  protected readonly activities = signal<Activity[]>([]);
  protected readonly contacts = signal<ContactSummary[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly editing = signal(false);
  protected readonly stageClosed = computed(() => {
    const o = this.opportunity();
    return !!o && !!this.catalogs()?.stages.find((s) => s.code === o.stageCode)?.closed;
  });

  protected readonly form = inject(FormBuilder).group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    amount: [0, [Validators.required, Validators.min(0)]],
    expectedCloseDate: ['', Validators.required],
    probability: [null as number | null, [Validators.min(0), Validators.max(100)]],
    primaryContactId: [null as number | null],
    ownerId: [null as number | null],
    description: [''],
  });

  ngOnInit(): void {
    const id = Number(this.id());
    this.api.opportunity(id).subscribe((o) => {
      this.opportunity.set(o);
      this.api.contacts({ companyId: o.companyId, size: 100 }).subscribe((p) => this.contacts.set(p.content));
    });
    this.api.stageHistory(id).subscribe((h) => this.history.set(h));
    this.api.quotes({ opportunityId: id, size: 50 }).subscribe((p) => this.quotes.set(p.content));
    this.loadActivities();
    if (this.auth.isManager()) {
      this.api.users().subscribe((u) => this.users.set(u.filter((x) => x.active)));
    }
  }

  protected loadActivities(): void {
    this.api.activities({ opportunityId: Number(this.id()), size: 100 }).subscribe((p) => this.activities.set(p.content));
  }

  protected stageName(code: string): string {
    return this.catalogs()?.stages.find((s) => s.code === code)?.name ?? code;
  }

  protected err(name: string): string | null {
    return fieldError(this.form.get(name));
  }

  protected openEdit(o: Opportunity): void {
    this.form.reset({
      title: o.title,
      amount: o.amount,
      expectedCloseDate: o.expectedCloseDate,
      probability: o.probability,
      primaryContactId: o.primaryContactId,
      ownerId: o.owner.id,
      description: o.description ?? '',
    });
    // En etapas cerradas la probabilidad es fija (100 % o 0 %).
    if (this.stageClosed()) {
      this.form.controls.probability.disable();
    } else {
      this.form.controls.probability.enable();
    }
    this.editing.set(true);
  }

  protected save(): void {
    this.form.markAllAsTouched();
    const o = this.opportunity();
    if (this.form.invalid || !o) return;
    const v = this.form.getRawValue();
    this.api
      .updateOpportunity(o.id, {
        title: v.title!.trim(),
        amount: v.amount!,
        expectedCloseDate: v.expectedCloseDate!,
        probability: this.stageClosed() ? null : v.probability,
        primaryContactId: v.primaryContactId,
        ownerId: this.auth.isManager() ? v.ownerId : null,
        description: v.description?.trim() || null,
        version: o.version,
      })
      .subscribe({
        next: (updated) => {
          this.opportunity.set(updated);
          this.editing.set(false);
          this.notifications.success('Oportunidad actualizada.');
        },
        error: (e) => applyServerErrors(this.form, e),
      });
  }

  protected toggle(a: Activity): void {
    this.api.completeActivity(a.id, !a.completedAt).subscribe(() => this.loadActivities());
  }

  protected remove(o: Opportunity): void {
    if (!confirm(`¿Eliminar la oportunidad "${o.title}" con sus cotizaciones y actividades?`)) return;
    this.api.deleteOpportunity(o.id).subscribe(() => {
      this.notifications.success('Oportunidad eliminada.');
      void this.router.navigate(['/pipeline']);
    });
  }
}
