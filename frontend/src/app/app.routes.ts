import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage), title: 'Ingresar · CRM Ventas' },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'pipeline' },
      { path: 'pipeline', loadComponent: () => import('./features/pipeline/board.page').then((m) => m.BoardPage), title: 'Pipeline · CRM Ventas' },
      { path: 'forecast', loadComponent: () => import('./features/forecast/forecast.page').then((m) => m.ForecastPage), title: 'Forecast · CRM Ventas' },
      { path: 'companies', loadComponent: () => import('./features/companies/companies.page').then((m) => m.CompaniesPage), title: 'Empresas · CRM Ventas' },
      { path: 'companies/:id', loadComponent: () => import('./features/companies/company-detail.page').then((m) => m.CompanyDetailPage), title: 'Empresa · CRM Ventas' },
      { path: 'contacts/:id', loadComponent: () => import('./features/contacts/contact-detail.page').then((m) => m.ContactDetailPage), title: 'Contacto · CRM Ventas' },
      { path: 'opportunities/:id', loadComponent: () => import('./features/opportunities/opportunity-detail.page').then((m) => m.OpportunityDetailPage), title: 'Oportunidad · CRM Ventas' },
      { path: 'activities', loadComponent: () => import('./features/activities/activities.page').then((m) => m.ActivitiesPage), title: 'Agenda · CRM Ventas' },
      { path: 'quotes/new', loadComponent: () => import('./features/quotes/quote-editor.page').then((m) => m.QuoteEditorPage), title: 'Nueva cotización · CRM Ventas' },
      { path: 'quotes/:id', loadComponent: () => import('./features/quotes/quote-editor.page').then((m) => m.QuoteEditorPage), title: 'Cotización · CRM Ventas' },
      { path: 'products', loadComponent: () => import('./features/products/products.page').then((m) => m.ProductsPage), title: 'Productos · CRM Ventas' },
      {
        path: 'privacy',
        canActivate: [roleGuard('MANAGER', 'ADMIN')],
        loadComponent: () => import('./features/privacy/privacy.page').then((m) => m.PrivacyPage),
        title: 'Privacidad · CRM Ventas',
      },
      {
        path: 'users',
        canActivate: [roleGuard('MANAGER', 'ADMIN')],
        loadComponent: () => import('./features/users/users.page').then((m) => m.UsersPage),
        title: 'Usuarios · CRM Ventas',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
