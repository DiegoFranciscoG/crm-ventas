import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { Product } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { Modal } from '../../shared/modal';
import { applyServerErrors, fieldError } from '../../shared/problem';

@Component({
  selector: 'app-products',
  imports: [CurrencyPipe, Modal, ReactiveFormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Productos y servicios</h1>
          <p>Tarifas de IVA según la Tabla 17 de la ficha técnica del SRI (15 % vigente).</p>
        </div>
        @if (auth.isManager()) { <button class="btn btn-primary" type="button" (click)="open(null)">+ Producto</button> }
      </div>
      <div class="card table-wrap">
        <table>
          <thead><tr><th>SKU</th><th>Nombre</th><th class="num">Precio</th><th>IVA</th><th>Estado</th>@if (auth.isManager()) { <th></th> }</tr></thead>
          <tbody>
            @for (p of products(); track p.id) {
              <tr>
                <td class="num">{{ p.sku }}</td>
                <td><strong>{{ p.name }}</strong><div class="small muted">{{ p.description ?? '' }}</div></td>
                <td class="num">{{ p.unitPrice | currency }}</td>
                <td>{{ p.vatPercentage }} %</td>
                <td>@if (p.active) { <span class="badge badge-good">✓ Activo</span> } @else { <span class="badge">Inactivo</span> }</td>
                @if (auth.isManager()) { <td><button class="btn btn-sm" type="button" (click)="open(p)">Editar</button></td> }
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    <app-modal [open]="editing()" [heading]="current() ? 'Editar producto' : 'Nuevo producto'" (closed)="editing.set(false)">
      <form class="form" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <div class="form-grid">
          <div class="field"><label for="p-sku">SKU</label><input id="p-sku" formControlName="sku" maxlength="40" />
            @if (err('sku'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="p-price">Precio unitario (USD)</label><input id="p-price" type="number" min="0" step="0.01" formControlName="unitPrice" />
            @if (err('unitPrice'); as e) { <span class="error">{{ e }}</span> }</div>
          <div class="field"><label for="p-vat">IVA</label>
            <select id="p-vat" formControlName="vatRateCode">
              @for (v of catalogs()?.vatRates ?? []; track v.code) { <option [value]="v.code">{{ v.description }} (código SRI {{ v.code }})</option> }
            </select></div>
        </div>
        <div class="field"><label for="p-name">Nombre</label><input id="p-name" formControlName="name" maxlength="200" />
          @if (err('name'); as e) { <span class="error">{{ e }}</span> }</div>
        <div class="field"><label for="p-desc">Descripción</label><textarea id="p-desc" formControlName="description" maxlength="1000"></textarea></div>
        <label class="row"><input type="checkbox" formControlName="active" /> Activo (disponible para cotizar)</label>
        <div class="form-actions">
          <button class="btn" type="button" (click)="editing.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="submit">Guardar</button>
        </div>
      </form>
    </app-modal>
  `,
})
export class ProductsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);
  protected readonly catalogs = toSignal(this.api.catalogs());
  protected readonly products = signal<Product[]>([]);
  protected readonly editing = signal(false);
  protected readonly current = signal<Product | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    sku: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9-]{2,40}$/)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    unitPrice: [0, [Validators.required, Validators.min(0)]],
    vatRateCode: ['4', Validators.required],
    active: [true],
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.api.products(this.auth.isManager()).subscribe((p) => this.products.set(p));
  }

  protected err(name: string): string | null {
    return fieldError(this.form.get(name));
  }

  protected open(product: Product | null): void {
    this.current.set(product);
    this.form.reset(
      product
        ? { sku: product.sku, name: product.name, description: product.description ?? '', unitPrice: product.unitPrice, vatRateCode: product.vatRateCode, active: product.active }
        : { sku: '', name: '', description: '', unitPrice: 0, vatRateCode: '4', active: true },
    );
    this.editing.set(true);
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.api.saveProduct(this.current()?.id ?? null, { ...v, description: v.description.trim() || null }).subscribe({
      next: () => {
        this.editing.set(false);
        this.notifications.success('Producto guardado.');
        this.load();
      },
      error: (e) => applyServerErrors(this.form, e),
    });
  }
}
