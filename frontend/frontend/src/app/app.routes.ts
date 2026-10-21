import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell/shell.component';
import { AuthLayoutComponent } from './layout/auth-layout/auth-layout.component';
import { BoasVindasComponent } from './pages/boas-vindas/boas-vindas.component';
import { LoginComponent } from './pages/login/login.component';
import { CadastroComponent } from './pages/cadastro/cadastro.component';
import { EncontrarMentoresComponent } from './pages/encontrar-mentores/encontrar-mentores.component';
import { EmBreveComponent } from './pages/em-breve/em-breve.component';
import { StartupListComponent } from './startups/startup-list.component';
import { StartupFormComponent } from './startups/startup-form.component';
import { MentorListComponent } from './mentores/mentor-list.component';
import { MentorFormComponent } from './mentores/mentor-form.component';

export const routes: Routes = [
  // Telas públicas (sem sidebar/topbar)
  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      { path: 'bem-vindo', component: BoasVindasComponent, title: 'Bem-vindo | Ponti' },
      { path: 'login', component: LoginComponent, title: 'Login | Ponti' },
      { path: 'cadastro', component: CadastroComponent, title: 'Cadastro | Ponti' },

      // TEMPORÁRIO: telas ainda não desenhadas
      { path: 'cadastro/startup', component: EmBreveComponent, data: { titulo: 'Cadastro de Startup' }, title: 'Cadastro de Startup | Ponti' },
      { path: 'cadastro/mentor', component: EmBreveComponent, data: { titulo: 'Cadastro de Mentor' }, title: 'Cadastro de Mentor | Ponti' },
      { path: 'recuperar-senha', component: EmBreveComponent, data: { titulo: 'Recuperar senha' }, title: 'Recuperar senha | Ponti' },
    ],
  },

  // Área logada
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
