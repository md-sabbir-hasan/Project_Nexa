import { Routes } from '@angular/router';

export const CHART_OF_ACCOUNTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/account-list/account-list.component')
        .then(m => m.AccountListComponent)
  }
];