import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LayoutComponent } from './layout/layout.component';

export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent)
      },
      {
        path: 'accounts',
        loadChildren: () =>
          import('./features/account/account.routes')
            .then(m => m.ACCOUNT_ROUTES)
      },
      {
        path: 'party',
        loadChildren: () =>
          import('./features/party/party.routes')
            .then(m => m.PARTY_ROUTES)
      },
      {
        path: 'invoice',
        loadChildren: () =>
          import('./features/invoice/invoice.routes')
            .then(m => m.INVOICE_ROUTES)
      },
      {
        path: 'vendor-bill',
        loadChildren: () =>
          import('./features/vendor-bill/vendor-bill.routes')
            .then(m => m.VENDOR_BILL_ROUTES)
      },
      {
        path: 'payment',
        loadChildren: () =>
          import('./features/payment/payment.routes')
            .then(m => m.PAYMENT_ROUTES)
      },
      {
        path: 'report',
        loadChildren: () =>
          import('./features/report/report.routes')
            .then(m => m.REPORT_ROUTES)
      },
      {
        path: 'admin',
        loadChildren: () =>
          import('./features/administration/admin.routes')
            .then(m => m.ADMIN_ROUTES)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'auth/login'
  }
];