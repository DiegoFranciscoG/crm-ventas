import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { ACTIVITY_LABELS } from '../../core/labels';
import { Activity } from '../../core/models';

type Filter = 'pending' | 'done';

@Component({
  selector: 'app-activities',
  imports: [DatePipe, RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Agenda</h1>
          <p>Sus llamadas, reuniones y tareas. Los recordatorios llegan por correo a la hora indicada.</p>
        </div>
      </div>
      <div class="card stack">
        <div class="tabs" role="tablist">
          <button role="tab" type="button" [attr.aria-selected]="filter() === 'pending'" (click)="setFilter('pending')">Pendientes</button>
          <button role="tab" type="button" [attr.aria-selected]="filter() === 'done'" (click)="setFilter('done')">Completadas</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th></th><th>Actividad</th><th>Relacionada con</th><th>Fecha</th><th>Estado</th></tr></thead>
            <tbody>
              @for (a of activities(); track a.id) {
                <tr>
                  <td><input type="checkbox" [checked]="!!a.completedAt" (change)="toggle(a)" [attr.aria-label]="'Completar ' + a.subject" /></td>
                  <td><strong>{{ a.subject }}</strong><div class="small muted">{{ labels[a.type] }}{{ a.contactName ? ' con ' + a.contactName : '' }}</div></td>
                  <td>
                    @if (a.opportunityId) { <a [routerLink]="['/opportunities', a.opportunityId]">{{ a.opportunityTitle }}</a> }
                    @else if (a.companyId) { <a [routerLink]="['/companies', a.companyId]">{{ a.companyName }}</a> }
                  </td>
                  <td>{{ a.dueAt ? (a.dueAt | date: 'EEE dd/MM HH:mm') : 'Sin fecha' }}</td>
                  <td>
                    @if (a.overdue) { <span class="badge badge-warn">⚠ Atrasada</span> }
                    @else if (a.completedAt) { <span class="badge badge-good">✓ Hecha</span> }
                    @if (a.reminderSentAt) { <span class="badge">✉ Recordatorio enviado</span> }
                    @else if (a.remindAt && !a.completedAt) { <span class="badge">🔔 {{ a.remindAt | date: 'dd/MM HH:mm' }}</span> }
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="5" class="empty">{{ filter() === 'pending' ? 'No tiene actividades pendientes.' : 'Aún no hay actividades completadas.' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class ActivitiesPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly labels = ACTIVITY_LABELS;
  protected readonly filter = signal<Filter>('pending');
  protected readonly activities = signal<Activity[]>([]);

  ngOnInit(): void {
    this.load();
  }

  protected setFilter(filter: Filter): void {
    this.filter.set(filter);
    this.load();
  }

  protected toggle(a: Activity): void {
    this.api.completeActivity(a.id, !a.completedAt).subscribe(() => this.load());
  }

  private load(): void {
    this.api.activities({ completed: this.filter() === 'done', size: 100 }).subscribe((p) => this.activities.set(p.content));
  }
}
