import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { ROLE_LABELS } from '../core/labels';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  managersOnly?: boolean;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  styleUrl: './shell.css',
  template: `
    <div class="shell" [class.nav-open]="navOpen()">
      <aside class="sidebar" aria-label="Navegación principal">
        <div class="brand">
          <span class="logo" aria-hidden="true">◆</span>
          <div>
            <strong>CRM Ventas</strong>
            <span class="small muted">B2B · Ecuador</span>
          </div>
        </div>
        <nav>
          @for (item of items(); track item.path) {
            <a [routerLink]="item.path" routerLinkActive="active" (click)="navOpen.set(false)">
              <span class="icon" aria-hidden="true">{{ item.icon }}</span>{{ item.label }}
            </a>
          }
        </nav>
        <div class="user">
          <div>
            <strong>{{ auth.user()?.fullName }}</strong>
            <span class="small muted">{{ roleLabel() }}</span>
          </div>
          <button class="btn btn-sm" type="button" (click)="auth.logout()">Salir</button>
        </div>
      </aside>
      <div class="main">
        <header class="topbar">
          <button class="btn btn-ghost menu" type="button" (click)="navOpen.set(!navOpen())" [attr.aria-expanded]="navOpen()">
            ☰ <span class="sr-only">Menú</span>
          </button>
          <strong>CRM Ventas</strong>
        </header>
        <main>
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly navOpen = signal(false);

  private readonly allItems: NavItem[] = [
    { path: '/pipeline', label: 'Pipeline', icon: '▦' },
    { path: '/forecast', label: 'Forecast', icon: '▤' },
    { path: '/activities', label: 'Agenda', icon: '◷' },
    { path: '/companies', label: 'Empresas', icon: '▣' },
    { path: '/products', label: 'Productos', icon: '▢' },
    { path: '/privacy', label: 'Privacidad LOPDP', icon: '◈', managersOnly: true },
    { path: '/users', label: 'Usuarios', icon: '◉', managersOnly: true },
  ];

  protected readonly items = computed(() =>
    this.allItems.filter((item) => !item.managersOnly || this.auth.isManager()),
  );

  protected readonly roleLabel = computed(() => {
    const role = this.auth.user()?.role;
    return role ? ROLE_LABELS[role] : '';
  });
}
