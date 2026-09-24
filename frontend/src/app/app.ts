import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificationService } from './core/notification.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `
    <router-outlet />
    <div class="toasts" aria-live="polite">
      @for (toast of notifications.toasts(); track toast.id) {
        <div class="toast" [class]="'toast toast-' + toast.kind" role="status">
          <span class="spacer">{{ toast.message }}</span>
          <button class="btn btn-ghost btn-sm" type="button" (click)="notifications.dismiss(toast.id)" aria-label="Cerrar aviso">✕</button>
        </div>
      }
    </div>
  `,
})
export class App {
  protected readonly notifications = inject(NotificationService);
}
