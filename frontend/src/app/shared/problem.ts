import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ProblemDetail } from '../core/models';

/** Aplica los errores de validación del backend (ProblemDetail.errors) a los controles del formulario. */
export function applyServerErrors(form: FormGroup, error: unknown): string | null {
  if (!(error instanceof HttpErrorResponse) || error.status !== 400) {
    return null;
  }
  const problem = (error.error ?? {}) as ProblemDetail;
  let unmatched: string | null = problem.detail ?? 'Revise los datos.';
  for (const [field, message] of Object.entries(problem.errors ?? {})) {
    const control: AbstractControl | null = form.get(field.replace(/\[(\d+)\]/g, '.$1'));
    if (control) {
      control.setErrors({ server: message });
      control.markAsTouched();
    } else {
      unmatched = message;
    }
  }
  return unmatched;
}

export function fieldError(control: AbstractControl | null): string | null {
  if (!control || !control.touched || !control.errors) {
    return null;
  }
  const errors = control.errors;
  if (errors['server']) return errors['server'];
  if (errors['identification']) return errors['identification'];
  if (errors['required']) return 'Este campo es obligatorio.';
  if (errors['email']) return 'Ingrese un correo válido.';
  if (errors['minlength']) return `Mínimo ${errors['minlength'].requiredLength} caracteres.`;
  if (errors['maxlength']) return `Máximo ${errors['maxlength'].requiredLength} caracteres.`;
  if (errors['min']) return `El valor mínimo es ${errors['min'].min}.`;
  if (errors['max']) return `El valor máximo es ${errors['max'].max}.`;
  if (errors['pattern']) return 'Formato inválido.';
  return 'Valor inválido.';
}

/** Convierte un valor de <input type="datetime-local"> (hora local) a ISO-8601 UTC. */
export function localToIso(value: string | null | undefined): string | null {
  return value ? new Date(value).toISOString() : null;
}

/** Convierte un instante ISO a valor de <input type="datetime-local"> en la hora local del navegador. */
export function isoToLocal(value: string | null | undefined): string {
  if (!value) return '';
  const date = new Date(value);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function todayIso(offsetDays = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
