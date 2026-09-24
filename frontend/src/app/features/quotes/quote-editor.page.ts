import { CurrencyPipe, DatePipe, LowerCasePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, saveBlob } from '../../core/api/api.service';
import { QUOTE_STATUS_LABELS } from '../../core/labels';
import { ContactSummary, Opportunity, Product, Quote, QuoteStatus } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { applyServerErrors, fieldError } from '../../shared/problem';
import { computeTotals } from './quote-math';

const NEXT_STATUS: Record<QuoteStatus, QuoteStatus[]> = {
  DRAFT: ['SENT'],
  SENT: ['ACCEPTED', 'REJECTED', 'EXPIRED', 'DRAFT'],
  ACCEPTED: [],
  REJECTED: [],
  EXPIRED: [],
};

@Component({
  selector: 'app-quote-editor',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe, LowerCasePipe],
  styleUrl: './quote-editor.page.css',
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          @if (opportunity(); as o) { <a [routerLink]="['/opportunities', o.id]" class="small">← {{ o.title }}</a> }
          <h1>{{ quote()?.number ?? 'Nueva cotización' }}</h1>
          @if (quote(); as q) {
            <p><span class="badge badge-accent">{{ statusLabels[q.status] }}</span> · {{ q.companyName }} · creada por {{ q.createdBy }}</p>
          } @else if (opportunity(); as o) {
            <p>{{ o.companyName }}</p>
          }
        </div>
        <div class="row">
          @if (quote(); as q) {
            <button class="btn" type="button" (click)="downloadPdf(q)">⬇ PDF</button>
            @for (next of nextStatuses(); track next) {
              <button class="btn" type="button" (click)="changeStatus(q, next)">Marcar {{ statusLabels[next] | lowercase }}</button>
            }
          }
        </div>
      </div>

      <form class="stack" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <fieldset class="card form" [disabled]="!editable()">
          <div class="form-grid">
            <div class="field">
              <label for="q-contact">Dirigida a</label>
              <select id="q-contact" formControlName="contactId">
                <option [ngValue]="null">La empresa (sin contacto)</option>
                @for (c of contacts(); track c.id) { <option [ngValue]="c.id">{{ c.fullName }}</option> }
              </select>
              <span class="hint">Requiere el consentimiento "Envío de cotizaciones" del contacto.</span>
              @if (err('contactId'); as e) { <span class="error">{{ e }}</span> }
            </div>
            <div class="field">
              <label for="q-issue">Emisión</label>
              <input id="q-issue" type="date" formControlName="issueDate" />
            </div>
            <div class="field">
              <label for="q-valid">Válida hasta</label>
              <input id="q-valid" type="date" formControlName="validUntil" />
            </div>
          </div>
        </fieldset>

        <fieldset class="card" [disabled]="!editable()">
          <div class="card-header">
            <h2>Ítems</h2>
            <button class="btn btn-sm" type="button" (click)="addItem()">+ Ítem</button>
          </div>
          <div class="table-wrap">
            <table class="items" formArrayName="items">
              <thead>
                <tr><th>Producto / descripción</th><th class="num">Cant.</th><th class="num">P. unitario</th><th class="num">Desc. %</th><th>IVA</th><th class="num">Subtotal</th><th></th></tr>
              </thead>
              <tbody>
                @for (item of items.controls; track item; let i = $index) {
                  <tr [formGroupName]="i">
                    <td class="desc">
                      <select formControlName="productId" (change)="pickProduct(i)" [attr.aria-label]="'Producto de la línea ' + (i + 1)">
                        <option [ngValue]="null">Ítem libre</option>
                        @for (p of products(); track p.id) { <option [ngValue]="p.id">{{ p.sku }} · {{ p.name }}</option> }
                      </select>
                      <input formControlName="description" placeholder="Descripción" maxlength="500" [attr.aria-label]="'Descripción de la línea ' + (i + 1)" />
                    </td>
                    <td><input type="number" min="0.01" step="0.01" formControlName="quantity" class="num-input" [attr.aria-label]="'Cantidad línea ' + (i + 1)" /></td>
                    <td><input type="number" min="0" step="0.01" formControlName="unitPrice" class="num-input" [attr.aria-label]="'Precio línea ' + (i + 1)" /></td>
                    <td><input type="number" min="0" max="100" step="0.01" formControlName="discountPercent" class="num-input" [attr.aria-label]="'Descuento línea ' + (i + 1)" /></td>
                    <td>
                      <select formControlName="vatRateCode" [attr.aria-label]="'IVA línea ' + (i + 1)">
                        @for (v of catalogs()?.vatRates ?? []; track v.code) { <option [value]="v.code">{{ v.description }}</option> }
                      </select>
                    </td>
                    <td class="num">{{ preview().lines[i]?.net ?? 0 | currency }}</td>
                    <td><button class="btn btn-ghost btn-sm" type="button" (click)="removeItem(i)" [disabled]="items.length === 1" aria-label="Quitar ítem">✕</button></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <div class="totals">
            <div><span>Subtotal</span><span class="num">{{ preview().subtotal | currency }}</span></div>
            <div><span>Descuento</span><span class="num">-{{ preview().discount | currency }}</span></div>
            @for (r of preview().byRate; track r.rate) {
              <div><span>Base IVA {{ r.rate }} %</span><span class="num">{{ r.base | currency }}</span></div>
              @if (r.rate > 0) { <div><span>IVA {{ r.rate }} %</span><span class="num">{{ r.tax | currency }}</span></div> }
            }
            <div class="grand"><span>Total USD</span><span class="num">{{ preview().total | currency }}</span></div>
            <span class="small muted">Vista previa; el servidor recalcula y guarda los valores definitivos.</span>
          </div>
        </fieldset>

        <fieldset class="card form" [disabled]="!editable()">
          <div class="field">
            <label for="q-notes">Observaciones</label>
            <textarea id="q-notes" formControlName="notes" maxlength="2000"></textarea>
          </div>
        </fieldset>

        @if (serverError()) { <p class="notice notice-danger">{{ serverError() }}</p> }
        @if (editable()) {
          <div class="form-actions">
            <button class="btn btn-primary" type="submit" [disabled]="saving()">{{ quote() ? 'Guardar cambios' : 'Crear cotización' }}</button>
          </div>
        } @else if (quote(); as q) {
          <p class="notice">Emitida el {{ q.issueDate | date: 'dd/MM/yyyy' }}. Solo las cotizaciones en borrador se pueden editar.</p>
        }
      </form>
    </div>
  `,
})
export class QuoteEditorPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  readonly id = input<string>();
  readonly opportunityId = input<string>();

  protected readonly statusLabels = QUOTE_STATUS_LABELS;
  protected readonly catalogs = toSignal(this.api.catalogs());
  protected readonly products = signal<Product[]>([]);
  protected readonly contacts = signal<ContactSummary[]>([]);
  protected readonly opportunity = signal<Opportunity | null>(null);
  protected readonly quote = signal<Quote | null>(null);
  protected readonly saving = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly editable = computed(() => !this.quote() || this.quote()!.status === 'DRAFT');
  protected readonly nextStatuses = computed(() => (this.quote() ? NEXT_STATUS[this.quote()!.status] : []));

  protected readonly form = this.fb.group({
    contactId: [null as number | null],
    issueDate: [''],
    validUntil: [''],
    notes: [''],
    items: this.fb.array<FormGroup>([]),
  });

  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.value });

  protected readonly preview = computed(() => {
    this.formValue();
    const rates = this.catalogs()?.vatRates ?? [];
    return computeTotals(
      this.items.controls.map((c) => {
        const v = c.getRawValue();
        return {
          quantity: Number(v.quantity) || 0,
          unitPrice: Number(v.unitPrice) || 0,
          discountPercent: Number(v.discountPercent) || 0,
          vatPercentage: rates.find((r) => r.code === v.vatRateCode)?.percentage ?? 0,
        };
      }),
    );
  });

  get items(): FormArray<FormGroup> {
    return this.form.controls.items;
  }

  ngOnInit(): void {
    this.api.products().subscribe((p) => this.products.set(p));
    const id = this.id();
    if (id) {
      this.api.quote(Number(id)).subscribe((q) => {
        this.fill(q);
        this.loadOpportunity(q.opportunityId);
      });
    } else if (this.opportunityId()) {
      this.addItem();
      this.loadOpportunity(Number(this.opportunityId()));
    }
  }

  private loadOpportunity(id: number): void {
    this.api.opportunity(id).subscribe((o) => {
      this.opportunity.set(o);
      this.api.contacts({ companyId: o.companyId, size: 100 }).subscribe((p) => this.contacts.set(p.content));
    });
  }

  private fill(q: Quote): void {
    this.quote.set(q);
    this.items.clear();
    q.items.forEach((i) =>
      this.items.push(this.itemGroup(i.productId, i.description, i.quantity, i.unitPrice, i.discountPercent, i.vatRateCode)),
    );
    this.form.patchValue({ contactId: q.contactId, issueDate: q.issueDate, validUntil: q.validUntil, notes: q.notes ?? '' });
  }

  private itemGroup(productId: number | null, description: string, quantity: number, unitPrice: number | null, discount: number, vat: string) {
    return this.fb.group({
      productId: [productId],
      description: [description, [Validators.required, Validators.maxLength(500)]],
      quantity: [quantity, [Validators.required, Validators.min(0.01)]],
      unitPrice: [unitPrice, [Validators.required, Validators.min(0)]],
      discountPercent: [discount, [Validators.min(0), Validators.max(100)]],
      vatRateCode: [vat, Validators.required],
    });
  }

  protected addItem(): void {
    this.items.push(this.itemGroup(null, '', 1, null, 0, '4'));
  }

  protected removeItem(index: number): void {
    this.items.removeAt(index);
  }

  protected pickProduct(index: number): void {
    const group = this.items.at(index);
    const product = this.products().find((p) => p.id === group.get('productId')!.value);
    if (product) {
      group.patchValue({ description: product.name, unitPrice: product.unitPrice, vatRateCode: product.vatRateCode });
    }
  }

  protected err(name: string): string | null {
    return fieldError(this.form.get(name));
  }

  protected save(): void {
    this.form.markAllAsTouched();
    const opportunity = this.opportunity();
    if (this.form.invalid || !opportunity) {
      this.serverError.set(this.form.invalid ? 'Complete la descripción, cantidad y precio de cada ítem.' : null);
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.serverError.set(null);
    this.api
      .saveQuote(this.quote()?.id ?? null, {
        opportunityId: opportunity.id,
        contactId: v.contactId,
        issueDate: v.issueDate || null,
        validUntil: v.validUntil || null,
        notes: v.notes?.trim() || null,
        items: v.items.map((i) => ({
          productId: i['productId'],
          description: i['description'],
          quantity: Number(i['quantity']),
          unitPrice: Number(i['unitPrice']),
          discountPercent: Number(i['discountPercent']) || 0,
          vatRateCode: i['vatRateCode'],
        })),
      })
      .subscribe({
        next: (q) => {
          this.saving.set(false);
          this.notifications.success(`Cotización ${q.number} guardada. Total: ${q.total.toFixed(2)} USD.`);
          if (!this.quote()) {
            void this.router.navigate(['/quotes', q.id]);
          }
          this.fill(q);
        },
        error: (e) => {
          this.saving.set(false);
          this.serverError.set(applyServerErrors(this.form, e));
        },
      });
  }

  protected changeStatus(q: Quote, status: QuoteStatus): void {
    this.api.changeQuoteStatus(q.id, status).subscribe((updated) => {
      this.fill(updated);
      this.notifications.success(`Cotización marcada como ${QUOTE_STATUS_LABELS[status].toLowerCase()}.`);
    });
  }

  protected downloadPdf(q: Quote): void {
    this.api.quotePdf(q.id).subscribe((blob) => saveBlob(blob, `${q.number}.pdf`));
  }
}
