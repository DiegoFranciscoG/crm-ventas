import { Component, ElementRef, effect, input, output, viewChild } from '@angular/core';

/** Diálogo modal accesible basado en el elemento nativo <dialog> (foco, Escape y fondo incluidos). */
@Component({
  selector: 'app-modal',
  template: `
    <dialog #dialog class="modal" [attr.aria-labelledby]="titleId" (close)="closed.emit()" (cancel)="closed.emit()">
      <div class="modal-body">
        <div class="card-header">
          <h2 [id]="titleId">{{ heading() }}</h2>
          <button class="btn btn-ghost btn-sm" type="button" (click)="closed.emit()" aria-label="Cerrar">✕</button>
        </div>
        <ng-content />
      </div>
    </dialog>
  `,
})
export class Modal {
  private static counter = 0;
  readonly open = input(false);
  readonly heading = input.required<string>();
  readonly closed = output<void>();
  protected readonly titleId = `modal-title-${++Modal.counter}`;
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const element = this.dialog().nativeElement;
      if (this.open() && !element.open) {
        element.showModal();
      } else if (!this.open() && element.open) {
        element.close();
      }
    });
  }
}
