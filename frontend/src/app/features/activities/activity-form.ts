import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api/api.service';
import { ACTIVITY_LABELS, entries } from '../../core/labels';
import { ActivityType, ContactSummary } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { applyServerErrors, fieldError, localToIso } from '../../shared/problem';

/** Alta rápida de actividad con recordatorio por correo al dueño. */
@Component({
  selector: 'app-activity-form',
  imports: [ReactiveFormsModule],
  template: `
    <form class="form quick" [formGroup]="form" (ngSubmit)="save()" novalidate>
      <div class="form-grid">
        <div class="field">
          <label for="a-type">Tipo</label>
          <select id="a-type" formControlName="type">
            @for (t of types; track t.value) { <option [value]="t.value">{{ t.label }}</option> }
          </select>
        </div>
        <div class="field">
          <label for="a-subject">Asunto</label>
          <input id="a-subject" formControlName="subject" maxlength="200" />
          @if (err('subject'); as e) { <span class="error">{{ e }}</span> }
        </div>
        <div class="field">
          <label for="a-contact">Con</label>
          <select id="a-contact" formControlName="contactId">
            <option [ngValue]="null">Sin contacto</option>
            @for (c of contacts(); track c.id) {
              <option [ngValue]="c.id">{{ c.fullName }}{{ c.commercialContactAllowed ? '' : ' (sin consentimiento)' }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="a-due">Fecha</label>
          <input id="a-due" type="datetime-local" formControlName="dueAt" />
        </div>
        <div class="field">
          <label for="a-remind">Recordarme</label>
          <input id="a-remind" type="datetime-local" formControlName="remindAt" />
          @if (err('remindAt'); as e) { <span class="error">{{ e }}</span> }
        </div>
      </div>
      @if (error()) { <p class="notice notice-danger">{{ error() }}</p> }
      <div class="form-actions"><button class="btn btn-primary" type="submit" [disabled]="saving()">Agregar actividad</button></div>
    </form>
  `,
})
export class ActivityForm {
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);
  readonly companyId = input<number | null>(null);
  readonly opportunityId = input<number | null>(null);
  readonly contacts = input<ContactSummary[]>([]);
  readonly saved = output<void>();

  protected readonly types = entries(ACTIVITY_LABELS);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).group({
    type: ['CALL' as ActivityType],
    subject: ['', [Validators.required, Validators.maxLength(200)]],
    contactId: [null as number | null],
    dueAt: [''],
    remindAt: [''],
  });

  protected err(name: string): string | null {
    return fieldError(this.form.get(name));
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.api
      .createActivity({
        type: v.type!,
        subject: v.subject!.trim(),
        contactId: v.contactId,
        dueAt: localToIso(v.dueAt),
        remindAt: localToIso(v.remindAt),
        companyId: this.companyId(),
        opportunityId: this.opportunityId(),
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.form.reset({ type: 'CALL', subject: '', contactId: null, dueAt: '', remindAt: '' });
          this.notifications.success('Actividad agregada.');
          this.saved.emit();
        },
        error: (e) => {
          this.saving.set(false);
          this.error.set(applyServerErrors(this.form, e));
        },
      });
  }
}
