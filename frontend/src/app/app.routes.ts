import { Routes } from '@angular/router';
import { autenticadoGuard } from './auth/auth.guard';
import { ShellComponent } from './layout/shell/shell.component';
import { AuthLayoutComponent } from './layout/auth-layout/auth-layout.component';
import { BoasVindasComponent } from './pages/boas-vindas/boas-vindas.component';
import { LoginComponent } from './pages/login/login.component';
import { CadastroComponent } from './pages/cadastro/cadastro.component';
import { CadastroStartupComponent } from './pages/cadastro-startup/cadastro-startup.component';
import { CadastroMentorComponent } from './pages/cadastro-mentor/cadastro-mentor.component';
import { EncontrarStartupsComponent } from './pages/encontrar-startups/encontrar-startups.component';
import { PerfilStartupComponent } from './pages/perfil-startup/perfil-startup.component';
import { PerfilMentorComponent } from './pages/perfil-mentor/perfil-mentor.component';
import { ConvitesComponent } from './pages/convites/convites.component';
import { EncontrarMentoresComponent } from './pages/encontrar-mentores/encontrar-mentores.component';
import { EmBreveComponent } from './pages/em-breve/em-breve.component';
import { StartupListComponent } from './startups/startup-list.component';
import { StartupFormComponent } from './startups/startup-form.component';
import { MentorListComponent } from './mentores/mentor-list.component';
import { MentorFormComponent } from './mentores/mentor-form.component';

export const routes: Routes = [
  // Primeira tela ao abrir o site
  { path: '', pathMatch: 'full', redirectTo: 'bem-vindo' },

  // Telas públicas (sem sidebar/topbar)
  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      { path: 'bem-vindo', component: BoasVindasComponent, title: 'Bem-vindo | Ponti' },
      { path: 'login', component: LoginComponent, title: 'Login | Ponti' },
      { path: 'cadastro', component: CadastroComponent, title: 'Cadastro | Ponti' },

      { path: 'cadastro/startup', component: CadastroStartupComponent, title: 'Cadastro de Startup | Ponti' },
      { path: 'cadastro/mentor', component: CadastroMentorComponent, title: 'Cadastro de Mentor | Ponti' },
      // TEMPORÁRIO: tela ainda não desenhada
      { path: 'recuperar-senha', component: EmBreveComponent, data: { titulo: 'Recuperar senha' }, title: 'Recuperar senha | Ponti' },
    ],
  },

  // Área logada (só entra quem fez login)
  {
    path: '',
    component: ShellComponent,
    canActivate: [autenticadoGuard],
    children: [
      { path: 'encontrar-mentores', component: EncontrarMentoresComponent, title: 'Encontrar Mentores | Ponti' },
      { path: 'convites', component: ConvitesComponent, title: 'Convites | Ponti' },
      { path: 'mentores/:id/perfil', component: PerfilMentorComponent, title: 'Perfil do mentor | Ponti' },
      { path: 'encontrar-startups', component: EncontrarStartupsComponent, title: 'Encontrar Startups | Ponti' },
      { path: 'startups/:id/perfil', component: PerfilStartupComponent, title: 'Perfil da startup | Ponti' },
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
