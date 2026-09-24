import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { AUDIT_ACTION_LABELS, REQUEST_CHANNEL_LABELS, REQUEST_TYPE_LABELS } from '../../core/labels';
import { AuditLogEntry, AuditResourceType, DataSubjectRequest, Page } from '../../core/models';

type Tab = 'requests' | 'log';

@Component({
  selector: 'app-privacy',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Privacidad (LOPDP)</h1>
          <p>Registro de solicitudes de derechos (Reglamento Art. 15) y log inmutable de accesos a datos personales.</p>
        </div>
      </div>
      <div class="card stack">
        <div class="tabs" role="tablist">
          <button role="tab" type="button" [attr.aria-selected]="tab() === 'requests'" (click)="tab.set('requests')">Solicitudes de derechos</button>
          <button role="tab" type="button" [attr.aria-selected]="tab() === 'log'" (click)="tab.set('log')">Log de accesos</button>
        </div>

        @if (tab() === 'requests') {
          <div class="table-wrap">
            <table>
              <thead><tr><th>#</th><th>Tipo</th><th>Canal</th><th>Recibida</th><th>Plazo (15 días)</th><th>Estado</th><th>Atención</th></tr></thead>
              <tbody>
                @for (r of requests()?.content ?? []; track r.id) {
                  <tr>
                    <td class="num">{{ r.id }}</td>
                    <td>{{ typeLabels[r.requestType] }}<div class="small muted">Contacto #{{ r.contactId }}</div></td>
                    <td>{{ channelLabels[r.channel] }}</td>
                    <td>{{ r.receivedAt | date: 'dd/MM/yyyy' }}</td>
                    <td>{{ r.dueAt | date: 'dd/MM/yyyy' }}</td>
                    <td>
                      @if (r.status === 'COMPLETED') { <span class="badge badge-good">✓ Atendida {{ r.completedAt | date: 'dd/MM' }}</span> }
                      @else { <span class="badge badge-warn">⚠ {{ r.status }}</span> }
                    </td>
                    <td class="small">{{ r.resolution }}<div class="muted">{{ r.handledBy }}</div></td>
                  </tr>
                } @empty {
                  <tr><td colspan="7" class="empty">Sin solicitudes registradas.</td></tr>
                }
              </tbody>
            </table>
          </div>
        } @else {
          <div class="row">
            <label class="sr-only" for="res">Recurso</label>
            <select id="res" [(ngModel)]="resourceType" (ngModelChange)="loadLog(0)" style="width:auto">
              <option [ngValue]="null">Todos los recursos</option>
              <option value="CONTACT">Contactos</option>
              <option value="COMPANY">Empresas (persona natural)</option>
              <option value="QUOTE">Cotizaciones</option>
            </select>
            <label class="row"><input type="checkbox" [(ngModel)]="onlyDenied" (ngModelChange)="loadLog(0)" /> Solo accesos denegados</label>
          </div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Recurso</th><th>Resultado</th><th>IP</th><th>Detalle</th></tr></thead>
              <tbody>
                @for (e of log()?.content ?? []; track e.id) {
                  <tr>
                    <td>{{ e.occurredAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                    <td>{{ e.userName }}</td>
                    <td>{{ actionLabels[e.action] }}</td>
                    <td>
                      @if (e.resourceType === 'CONTACT' && e.resourceId) { <a [routerLink]="['/contacts', e.resourceId]">Contacto #{{ e.resourceId }}</a> }
                      @else { {{ e.resourceType }} {{ e.resourceId ? '#' + e.resourceId : '' }} }
                    </td>
                    <td>@if (e.success) { <span class="badge badge-good">✓ Permitido</span> } @else { <span class="badge badge-danger">✕ Denegado</span> }</td>
                    <td class="small muted">{{ e.ipAddress }}</td>
                    <td class="small muted">{{ e.detail }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="7" class="empty">Sin registros en los últimos 30 días.</td></tr>
                }
              </tbody>
            </table>
          </div>
          @if (log(); as l) {
            @if (l.totalPages > 1) {
              <div class="row">
                <span class="spacer"></span>
                <button class="btn btn-sm" type="button" [disabled]="l.page === 0" (click)="loadLog(l.page - 1)">Anterior</button>
                <span class="small">Página {{ l.page + 1 }} de {{ l.totalPages }}</span>
                <button class="btn btn-sm" type="button" [disabled]="l.page + 1 >= l.totalPages" (click)="loadLog(l.page + 1)">Siguiente</button>
              </div>
            }
          }
        }
      </div>
    </div>
  `,
})
export class PrivacyPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly typeLabels = REQUEST_TYPE_LABELS;
  protected readonly channelLabels = REQUEST_CHANNEL_LABELS;
  protected readonly actionLabels = AUDIT_ACTION_LABELS;
  protected readonly tab = signal<Tab>('requests');
  protected readonly requests = signal<Page<DataSubjectRequest> | null>(null);
  protected readonly log = signal<Page<AuditLogEntry> | null>(null);
  protected resourceType: AuditResourceType | null = null;
  protected onlyDenied = false;

  ngOnInit(): void {
    this.api.dataSubjectRequests(0, 50).subscribe((p) => this.requests.set(p));
    this.loadLog(0);
  }

  protected loadLog(page: number): void {
    this.api
      .accessLog({ resourceType: this.resourceType, onlyDenied: this.onlyDenied, page, size: 25 })
      .subscribe((p) => this.log.set(p));
  }
}
