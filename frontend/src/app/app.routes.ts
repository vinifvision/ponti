import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell/shell.component';
import { EncontrarMentoresComponent } from './pages/encontrar-mentores/encontrar-mentores.component';
import { EmBreveComponent } from './pages/em-breve/em-breve.component';
import { StartupListComponent } from './startups/startup-list.component';
import { StartupFormComponent } from './startups/startup-form.component';
import { MentorListComponent } from './mentores/mentor-list.component';
import { MentorFormComponent } from './mentores/mentor-form.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'encontrar-mentores' },
      { path: 'encontrar-mentores', component: EncontrarMentoresComponent, title: 'Encontrar Mentores | Ponti' },
      { path: 'agenda', component: EmBreveComponent, data: { titulo: 'Agenda' }, title: 'Agenda | Ponti' },
      { path: 'perfil', component: EmBreveComponent, data: { titulo: 'Perfil' }, title: 'Perfil | Ponti' },
      {
        path: 'configuracoes',
        component: EmBreveComponent,
        data: { titulo: 'Configurações' },
        title: 'Configurações | Ponti',
      },

      // Telas de CRUD feitas para a atividade (continuam funcionando dentro do novo layout)
      { path: 'startups', component: StartupListComponent },
      { path: 'startups/novo', component: StartupFormComponent },
      { path: 'startups/:id/editar', component: StartupFormComponent },
      { path: 'mentores', component: MentorListComponent },
      { path: 'mentores/novo', component: MentorFormComponent },
      { path: 'mentores/:id/editar', component: MentorFormComponent },
    ],
  },
  { path: '**', redirectTo: '' },
];
