import { Routes } from '@angular/router';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: 'chart-of-accounts',
    loadChildren: () =>
      import('./chart-of-accounts/chart-of-accounts.routes')
        .then(m => m.CHART_OF_ACCOUNTS_ROUTES)
  },
  {
    path: '',
    redirectTo: 'chart-of-accounts',
    pathMatch: 'full'
  }
];