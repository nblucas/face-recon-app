import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'register' },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register').then((m) => m.Register),
  },
  {
    path: 'list',
    loadComponent: () => import('./pages/list/list').then((m) => m.List),
  },
  {
    path: 'verification',
    loadComponent: () => import('./pages/verification/verification').then((m) => m.Verification),
  },
  {
    path: 'identification',
    loadComponent: () =>
      import('./pages/identification/identification').then((m) => m.Identification),
  },
];
