import { CdkDrag, CdkDragDrop, CdkDropList, CdkDropListGroup, transferArrayItem } from '@angular/cdk/drag-drop';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { BoardColumn, CompanySummary, OpportunityCard, User } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Modal } from '../../shared/modal';
import { applyServerErrors, fieldError, todayIso } from '../../shared/problem';

interface PendingLoss {
  card: OpportunityCard;
  from: BoardColumn;
  to: BoardColumn;
}

@Component({
  selector: 'app-board',
  imports: [CdkDropListGroup, CdkDropList, CdkDrag, CurrencyPipe, DatePipe, RouterLink, Modal, ReactiveFormsModule, FormsModule],
  styleUrl: './board.page.css',
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Pipeline</h1>
          <p>Arrastre una tarjeta para cambiar de etapa: la probabilidad se ajusta sola y queda en el historial.</p>
        </div>
        <div class="row">
          @if (auth.isManager()) {
            <label class="sr-only" for="owner-filter">Vendedor</label>
            <select id="owner-filter" [ngModel]="ownerId()" (ngModelChange)="filterOwner($event)" class="owner-filter">
              <option [ngValue]="null">Todo el equipo</option>
              @for (user of users(); track user.id) {
                <option [ngValue]="user.id">{{ user.fullName }}</option>
              }
            </select>
          }
          <button class="btn btn-primary" type="button" (click)="openCreate()">+ Oportunidad</button>
        </div>
      </div>

      <div class="summary card">
        <div><span class="muted small">Pipeline abierto</span><strong>{{ openTotal() | currency }}</strong></div>
        <div><span class="muted small">Ponderado (monto × probabilidad)</span><strong>{{ weightedTotal() | currency }}</strong></div>
        <div><span class="muted small">Oportunidades abiertas</span><strong>{{ openCount() }}</strong></div>
      </div>

      @if (loading()) {
        <p class="muted">Cargando tablero…</p>
      } @else {
        <div class="board" cdkDropListGroup>
          @for (column of columns(); track column.stage.code) {
            <section class="column" [class.closed]="column.stage.closed" [attr.aria-label]="column.stage.name">
              <header>
                <div class="row">
                  <h2>{{ column.stage.name }}</h2>
                  <span class="badge">{{ column.opportunities.length }}</span>
                </div>
                <span class="small muted">
                  <span class="badge badge-accent" title="Probabilidad por defecto de la etapa">{{ column.stage.defaultProbability }} %</span>
                  {{ columnTotal(column) | currency }} · pond. {{ columnWeighted(column) | currency }}
                </span>
              </header>
              <div
                class="cards"
                cdkDropList
                [cdkDropListData]="column"
                (cdkDropListDropped)="drop($event)"
              >
                @for (card of column.opportunities; track card.id) {
                  <article class="opp" cdkDrag [cdkDragData]="card" [class.overdue]="card.overdue">
                    <a [routerLink]="['/opportunities', card.id]" class="title">{{ card.title }}</a>
                    <span class="small muted">{{ card.companyName }}</span>
                    <div class="row small">
                      <strong>{{ card.amount | currency }}</strong>
                      <span class="spacer"></span>
                      <span class="muted">{{ card.probability }} %</span>
                    </div>
                    <div class="row small">
                      @if (card.overdue) {
                        <span class="badge badge-warn">⚠ Vencida {{ card.expectedCloseDate | date: 'dd/MM' }}</span>
                      } @else {
                        <span class="muted">Cierre {{ card.expectedCloseDate | date: 'dd/MM/yyyy' }}</span>
                      }
                      <span class="spacer"></span>
                      @if (auth.isManager()) { <span class="muted">{{ initials(card.owner.fullName) }}</span> }
                    </div>
                    <label class="sr-only" [for]="'move-' + card.id">Mover {{ card.title }} a</label>
                    <select class="move" [id]="'move-' + card.id" (change)="moveWithKeyboard(card, column, $any($event.target).value)">
                      <option value="">Mover a…</option>
                      @for (target of columns(); track target.stage.code) {
                        @if (target.stage.code !== column.stage.code) {
                          <option [value]="target.stage.code">{{ target.stage.name }}</option>
                        }
                      }
                    </select>
                  </article>
                } @empty {
                  <p class="drop-hint small muted">Suelte aquí</p>
                }
              </div>
            </section>
          }
        </div>
      }
    </div>

    <app-modal [open]="!!pendingLoss()" heading="Motivo de pérdida" (closed)="cancelLoss()">
      <form class="form" (ngSubmit)="confirmLoss()">
        <p class="muted">Registrar el motivo ayuda a entender por qué se pierden ventas.</p>
        <div class="field">
          <label for="lost-reason">Motivo</label>
          <textarea id="lost-reason" name="lostReason" [(ngModel)]="lostReason" maxlength="300" required></textarea>
        </div>
        <div class="form-actions">
          <button class="btn" type="button" (click)="cancelLoss()">Cancelar</button>
          <button class="btn btn-danger" type="submit" [disabled]="!lostReason.trim()">Marcar como perdida</button>
        </div>
      </form>
    </app-modal>

    <app-modal [open]="creating()" heading="Nueva oportunidad" (closed)="creating.set(false)">
      <form class="form" [formGroup]="createForm" (ngSubmit)="create()" novalidate>
        <div class="field">
          <label for="opp-title">Título</label>
          <input id="opp-title" formControlName="title" maxlength="200" />
          @if (err('title'); as e) { <span class="error">{{ e }}</span> }
        </div>
        <div class="field">
          <label for="opp-company">Empresa</label>
          <select id="opp-company" formControlName="companyId">
            <option [ngValue]="null">Seleccione…</option>
            @for (company of companies(); track company.id) {
              <option [ngValue]="company.id">{{ company.legalName }}</option>
            }
          </select>
          @if (err('companyId'); as e) { <span class="error">{{ e }}</span> }
        </div>
        <div class="form-grid">
          <div class="field">
            <label for="opp-amount">Monto (USD, sin IVA)</label>
            <input id="opp-amount" type="number" min="0" step="0.01" formControlName="amount" />
            @if (err('amount'); as e) { <span class="error">{{ e }}</span> }
          </div>
          <div class="field">
            <label for="opp-close">Cierre estimado</label>
            <input id="opp-close" type="date" formControlName="expectedCloseDate" />
            @if (err('expectedCloseDate'); as e) { <span class="error">{{ e }}</span> }
          </div>
        </div>
        <div class="form-actions">
          <button class="btn" type="button" (click)="creating.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="submit" [disabled]="saving()">Crear</button>
        </div>
      </form>
    </app-modal>
  `,
})
export class BoardPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);

  protected readonly columns = signal<BoardColumn[]>([]);
  protected readonly loading = signal(true);
  protected readonly users = signal<User[]>([]);
  protected readonly ownerId = signal<number | null>(null);
  protected readonly pendingLoss = signal<PendingLoss | null>(null);
  protected readonly creating = signal(false);
  protected readonly saving = signal(false);
  protected readonly companies = signal<CompanySummary[]>([]);
  protected lostReason = '';

  protected readonly openColumns = computed(() => this.columns().filter((c) => !c.stage.closed));
  protected readonly openTotal = computed(() => this.openColumns().reduce((sum, c) => sum + this.columnTotal(c), 0));
  protected readonly weightedTotal = computed(() => this.openColumns().reduce((sum, c) => sum + this.columnWeighted(c), 0));
  protected readonly openCount = computed(() => this.openColumns().reduce((sum, c) => sum + c.opportunities.length, 0));

  protected readonly createForm = inject(FormBuilder).group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    companyId: [null as number | null, Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0)]],
    expectedCloseDate: [todayIso(30), Validators.required],
  });

  ngOnInit(): void {
    this.load();
    if (this.auth.isManager()) {
      this.api.users().subscribe((users) => this.users.set(users.filter((u) => u.active)));
    }
  }

  protected columnTotal(column: BoardColumn): number {
    return column.opportunities.reduce((sum, o) => sum + o.amount, 0);
  }

  protected columnWeighted(column: BoardColumn): number {
    return column.opportunities.reduce((sum, o) => sum + (o.amount * o.probability) / 100, 0);
  }

  protected initials(name: string): string {
    return name.split(' ').map((part) => part[0]).slice(0, 2).join('');
  }

  protected filterOwner(ownerId: number | null): void {
    this.ownerId.set(ownerId);
    this.load();
  }

  protected drop(event: CdkDragDrop<BoardColumn>): void {
    if (event.previousContainer === event.container) {
      return;
    }
    const card = event.item.data as OpportunityCard;
    this.move(card, event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);
  }

  protected moveWithKeyboard(card: OpportunityCard, from: BoardColumn, stageCode: string): void {
    const to = this.columns().find((c) => c.stage.code === stageCode);
    if (to) {
      this.move(card, from, to, from.opportunities.indexOf(card), 0);
    }
  }

  private move(card: OpportunityCard, from: BoardColumn, to: BoardColumn, fromIndex: number, toIndex: number): void {
    // Actualización optimista: la tarjeta se mueve de inmediato y se revierte si el servidor rechaza el cambio.
    transferArrayItem(from.opportunities, to.opportunities, fromIndex, toIndex);
    this.columns.update((cols) => [...cols]);
    if (to.stage.closed && !to.stage.won) {
      this.lostReason = '';
      this.pendingLoss.set({ card, from, to });
      return;
    }
    this.persist(card, to);
  }

  protected confirmLoss(): void {
    const pending = this.pendingLoss();
    if (!pending || !this.lostReason.trim()) return;
    this.pendingLoss.set(null);
    this.persist(pending.card, pending.to, this.lostReason.trim());
  }

  protected cancelLoss(): void {
    if (this.pendingLoss()) {
      this.pendingLoss.set(null);
      this.load();
    }
  }

  private persist(card: OpportunityCard, to: BoardColumn, lostReason?: string): void {
    this.api.changeStage(card.id, to.stage.code, card.version, lostReason).subscribe({
      next: (updated) => {
        card.version = updated.version;
        card.probability = updated.probability;
        card.overdue = card.overdue && !to.stage.closed;
        this.columns.update((cols) => [...cols]);
        this.notifications.success(`"${card.title}" pasó a ${to.stage.name} (${updated.probability} %).`);
      },
      error: () => this.load(),
    });
  }

  protected openCreate(): void {
    this.createForm.reset({ title: '', companyId: null, amount: null, expectedCloseDate: todayIso(30) });
    this.creating.set(true);
    if (this.companies().length === 0) {
      this.api.companies({ size: 100 }).subscribe((page) => this.companies.set(page.content));
    }
  }

  protected err(name: string): string | null {
    return fieldError(this.createForm.get(name));
  }

  protected create(): void {
    this.createForm.markAllAsTouched();
    if (this.createForm.invalid) return;
    const value = this.createForm.getRawValue();
    this.saving.set(true);
    this.api
      .createOpportunity({
        title: value.title!,
        companyId: value.companyId!,
        amount: value.amount!,
        expectedCloseDate: value.expectedCloseDate!,
      })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          this.creating.set(false);
          void this.router.navigate(['/opportunities', created.id]);
        },
        error: (e) => {
          this.saving.set(false);
          applyServerErrors(this.createForm, e);
        },
      });
  }

  private load(): void {
    this.api.board(this.ownerId()).subscribe({
      next: (columns) => {
        this.columns.set(columns);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
