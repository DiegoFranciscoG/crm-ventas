import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { Forecast, User } from '../../core/models';

type Preset = 'quarter' | 'next6' | 'year' | 'custom';
type SeriesKey = 'weighted' | 'commit' | 'closedWon';

interface Tooltip {
  x: number;
  y: number;
  month: string;
  values: { label: string; value: number; key: SeriesKey }[];
}

const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

@Component({
  selector: 'app-forecast',
  imports: [CurrencyPipe, DecimalPipe, FormsModule],
  styleUrl: './forecast.page.css',
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Forecast de ventas</h1>
          <p>Ponderado = monto × probabilidad de la etapa. Commit = negociación; best case = commit + cotización enviada.</p>
        </div>
      </div>

      <div class="filters card">
        <div class="field">
          <label for="preset">Período</label>
          <select id="preset" [ngModel]="preset()" (ngModelChange)="choosePreset($event)">
            <option value="quarter">Trimestre actual</option>
            <option value="next6">Próximos 6 meses</option>
            <option value="year">Año en curso</option>
            <option value="custom">Personalizado</option>
          </select>
        </div>
        @if (preset() === 'custom') {
          <div class="field">
            <label for="from">Desde</label>
            <input id="from" type="date" [ngModel]="from()" (ngModelChange)="from.set($event); load()" />
          </div>
          <div class="field">
            <label for="to">Hasta</label>
            <input id="to" type="date" [ngModel]="to()" (ngModelChange)="to.set($event); load()" />
          </div>
        }
        @if (auth.isManager()) {
          <div class="field">
            <label for="owner">Vendedor</label>
            <select id="owner" [ngModel]="ownerId()" (ngModelChange)="ownerId.set($event); load()">
              <option [ngValue]="null">Todo el equipo</option>
              @for (user of users(); track user.id) {
                <option [ngValue]="user.id">{{ user.fullName }}</option>
              }
            </select>
          </div>
        }
      </div>

      @if (data(); as f) {
        <section class="kpis" aria-label="Indicadores principales">
          <div class="card kpi hero">
            <span class="label">Pipeline ponderado</span>
            <span class="value">{{ f.totals.weightedPipeline | currency: 'USD' : 'symbol' : '1.0-0' }}</span>
            <span class="small muted">{{ f.totals.openCount }} oportunidades abiertas</span>
          </div>
          <div class="card kpi">
            <span class="label">Pipeline abierto</span>
            <span class="value">{{ f.totals.openPipeline | currency: 'USD' : 'symbol' : '1.0-0' }}</span>
          </div>
          <div class="card kpi">
            <span class="label">Commit</span>
            <span class="value">{{ f.totals.commit | currency: 'USD' : 'symbol' : '1.0-0' }}</span>
          </div>
          <div class="card kpi">
            <span class="label">Best case</span>
            <span class="value">{{ f.totals.bestCase | currency: 'USD' : 'symbol' : '1.0-0' }}</span>
          </div>
          <div class="card kpi">
            <span class="label">Ganado en el período</span>
            <span class="value">{{ f.totals.closedWon | currency: 'USD' : 'symbol' : '1.0-0' }}</span>
          </div>
          <div class="card kpi">
            <span class="label">Tasa de cierre</span>
            <span class="value">{{ f.totals.winRate | number: '1.0-1' }} %</span>
            <span class="small muted">{{ f.totals.wonCount }} ganadas · {{ f.totals.lostCount }} perdidas</span>
          </div>
        </section>

        <section class="card">
          <div class="card-header">
            <h2>Por mes</h2>
            <button class="btn btn-sm" type="button" (click)="showTable.set(!showTable())" [attr.aria-pressed]="showTable()">
              {{ showTable() ? 'Ver gráfico' : 'Ver tabla' }}
            </button>
          </div>
          <ul class="legend" aria-label="Leyenda">
            @for (s of series; track s.key) {
              <li><span class="swatch" [style.background]="s.color"></span>{{ s.label }}</li>
            }
          </ul>
          @if (showTable()) {
            <div class="table-wrap">
              <table>
                <thead>
                  <tr><th>Mes</th><th class="num">Pipeline</th><th class="num">Ponderado</th><th class="num">Commit</th><th class="num">Best case</th><th class="num">Ganado</th></tr>
                </thead>
                <tbody>
                  @for (m of f.byMonth; track m.month) {
                    <tr>
                      <td>{{ monthLabel(m.month) }}</td>
                      <td class="num">{{ m.pipeline | currency }}</td>
                      <td class="num">{{ m.weighted | currency }}</td>
                      <td class="num">{{ m.commit | currency }}</td>
                      <td class="num">{{ m.bestCase | currency }}</td>
                      <td class="num">{{ m.closedWon | currency }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <div class="chart" (mouseleave)="tooltip.set(null)">
              <div class="y-axis" aria-hidden="true">
                @for (tick of ticks(); track tick) {
                  <span [style.bottom.%]="(tick / scaleMax()) * 100">{{ compact(tick) }}</span>
                }
              </div>
              <div class="plot" role="img" [attr.aria-label]="chartSummary()">
                <div class="gridlines" aria-hidden="true">
                  @for (tick of ticks(); track tick) {
                    <div class="gridline" [class.baseline]="tick === 0" [style.bottom.%]="(tick / scaleMax()) * 100"></div>
                  }
                </div>
                <div class="groups">
                  @for (m of f.byMonth; track m.month) {
                    <div class="group" (mousemove)="showTooltip($event, m)">
                      <div class="bars">
                        @for (s of series; track s.key) {
                          <div class="bar" [style.height.%]="(m[s.key] / scaleMax()) * 100" [style.background]="s.color"></div>
                        }
                      </div>
                      <span class="x-label">{{ monthLabel(m.month) }}</span>
                    </div>
                  }
                </div>
              </div>
              @if (tooltip(); as t) {
                <div class="tooltip" [style.left.px]="t.x" [style.top.px]="t.y" role="tooltip">
                  <strong>{{ t.month }}</strong>
                  @for (v of t.values; track v.key) {
                    <div class="tooltip-row"><span class="swatch" [style.background]="colorOf(v.key)"></span>{{ v.label }}<span class="spacer"></span>{{ v.value | currency }}</div>
                  }
                </div>
              }
            </div>
          }
        </section>

        <div class="grid-2">
          <section class="card">
            <div class="card-header"><h2>Por etapa</h2><span class="small muted">Monto en el período</span></div>
            <div class="hbars">
              @for (stage of f.byStage; track stage.stageCode) {
                <div class="hbar-row">
                  <span class="hbar-label">{{ stage.stageName }} <span class="muted">({{ stage.count }})</span></span>
                  <div class="hbar-track">
                    <div class="hbar" [style.width.%]="stageMax() ? (stage.amount / stageMax()) * 100 : 0"></div>
                  </div>
                  <span class="num small">{{ stage.amount | currency: 'USD' : 'symbol' : '1.0-0' }}</span>
                </div>
              }
            </div>
          </section>

          @if (f.byOwner.length > 0) {
            <section class="card">
              <div class="card-header"><h2>Por vendedor</h2></div>
              <div class="table-wrap">
                <table>
                  <thead><tr><th>Vendedor</th><th class="num">Abiertas</th><th class="num">Ponderado</th><th class="num">Ganado</th></tr></thead>
                  <tbody>
                    @for (o of f.byOwner; track o.ownerId) {
                      <tr>
                        <td>{{ o.ownerName }}</td>
                        <td class="num">{{ o.openCount }}</td>
                        <td class="num">{{ o.weighted | currency }}</td>
                        <td class="num">{{ o.closedWon | currency }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </section>
          }
        </div>
      } @else {
        <p class="muted">Calculando forecast…</p>
      }
    </div>
  `,
})
export class ForecastPage implements OnInit {
  private readonly api = inject(ApiService);
  protected readonly auth = inject(AuthService);

  protected readonly series: { key: SeriesKey; label: string; color: string }[] = [
    { key: 'weighted', label: 'Ponderado', color: 'var(--series-1)' },
    { key: 'commit', label: 'Commit', color: 'var(--series-2)' },
    { key: 'closedWon', label: 'Ganado', color: 'var(--series-3)' },
  ];

  protected readonly preset = signal<Preset>('next6');
  protected readonly from = signal('');
  protected readonly to = signal('');
  protected readonly ownerId = signal<number | null>(null);
  protected readonly users = signal<User[]>([]);
  protected readonly data = signal<Forecast | null>(null);
  protected readonly showTable = signal(false);
  protected readonly tooltip = signal<Tooltip | null>(null);

  private readonly maxValue = computed(() => {
    const months = this.data()?.byMonth ?? [];
    return Math.max(0, ...months.flatMap((m) => this.series.map((s) => m[s.key])));
  });

  /** Escala con valores "limpios" (1, 2, 2.5, 5 × 10^n) para los ticks del eje. */
  protected readonly scaleMax = computed(() => {
    const max = this.maxValue();
    if (max <= 0) return 1;
    const magnitude = 10 ** Math.floor(Math.log10(max));
    const step = [1, 2, 2.5, 5, 10].map((f) => f * magnitude).find((s) => s * 4 >= max) ?? magnitude * 10;
    return step * Math.ceil(max / step);
  });

  protected readonly ticks = computed(() => {
    const top = this.scaleMax();
    return [0, 0.25, 0.5, 0.75, 1].map((f) => Math.round(top * f));
  });

  protected readonly stageMax = computed(() => Math.max(0, ...(this.data()?.byStage ?? []).map((s) => s.amount)));

  protected readonly chartSummary = computed(() => {
    const f = this.data();
    if (!f) return '';
    return `Gráfico de columnas por mes con ponderado, commit y ganado. Total ponderado ${Math.round(f.totals.weightedPipeline)} dólares. Use "Ver tabla" para los valores.`;
  });

  ngOnInit(): void {
    this.choosePreset('next6');
    if (this.auth.isManager()) {
      this.api.users().subscribe((users) => this.users.set(users.filter((u) => u.role === 'SALES_REP')));
    }
  }

  protected choosePreset(preset: Preset): void {
    this.preset.set(preset);
    const now = new Date();
    const iso = (d: Date) => d.toISOString().slice(0, 10);
    if (preset === 'quarter') {
      const start = new Date(now.getFullYear(), Math.floor(now.getMonth() / 3) * 3, 1);
      this.from.set(iso(start));
      this.to.set(iso(new Date(start.getFullYear(), start.getMonth() + 3, 0)));
    } else if (preset === 'next6') {
      this.from.set(iso(new Date(now.getFullYear(), now.getMonth(), 1)));
      this.to.set(iso(new Date(now.getFullYear(), now.getMonth() + 6, 0)));
    } else if (preset === 'year') {
      this.from.set(`${now.getFullYear()}-01-01`);
      this.to.set(`${now.getFullYear()}-12-31`);
    }
    this.load();
  }

  protected load(): void {
    if (!this.from() || !this.to()) return;
    this.api.forecast(this.from(), this.to(), this.ownerId()).subscribe((f) => this.data.set(f));
  }

  protected monthLabel(month: string): string {
    const [year, m] = month.split('-');
    return `${MONTHS[Number(m) - 1]} ${year.slice(2)}`;
  }

  protected compact(value: number): string {
    return value >= 1000 ? `${(value / 1000).toLocaleString('es-EC', { maximumFractionDigits: 1 })} k` : String(value);
  }

  protected colorOf(key: SeriesKey): string {
    return this.series.find((s) => s.key === key)!.color;
  }

  protected showTooltip(event: MouseEvent, month: Forecast['byMonth'][number]): void {
    const chart = (event.currentTarget as HTMLElement).closest('.chart')!.getBoundingClientRect();
    this.tooltip.set({
      x: Math.min(event.clientX - chart.left + 12, chart.width - 200),
      y: event.clientY - chart.top + 12,
      month: this.monthLabel(month.month),
      values: this.series.map((s) => ({ key: s.key, label: s.label, value: month[s.key] })),
    });
  }
}
