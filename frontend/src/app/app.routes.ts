import { Routes } from '@angular/router';
import { StartupListComponent } from './startups/startup-list.component';
import { StartupFormComponent } from './startups/startup-form.component';
import { MentorListComponent } from './mentores/mentor-list.component';
import { MentorFormComponent } from './mentores/mentor-form.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'startups' },
  { path: 'startups', component: StartupListComponent },
  { path: 'startups/novo', component: StartupFormComponent },
  { path: 'startups/:id/editar', component: StartupFormComponent },
  { path: 'mentores', component: MentorListComponent },
  { path: 'mentores/novo', component: MentorFormComponent },
  { path: 'mentores/:id/editar', component: MentorFormComponent },
];
