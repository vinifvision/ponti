import { Routes } from '@angular/router';
import { autenticadoGuard, tipoGuard } from './auth/auth.guard';
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
import { AgendarMentoriaComponent } from './pages/agendar-mentoria/agendar-mentoria.component';
import { MentoriasComponent } from './pages/mentorias/mentorias.component';
import { MinhaAgendaComponent } from './pages/minha-agenda/minha-agenda.component';
import { EncontrarMentoresComponent } from './pages/encontrar-mentores/encontrar-mentores.component';
import { EmBreveComponent } from './pages/em-breve/em-breve.component';
import { MeuPerfilComponent } from './pages/meu-perfil/meu-perfil.component';
import { EditarPerfilComponent } from './pages/editar-perfil/editar-perfil.component';
import { ConfiguracoesComponent } from './pages/configuracoes/configuracoes.component';

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
      // Telas só de startup
      { path: 'encontrar-mentores', component: EncontrarMentoresComponent, canActivate: [tipoGuard('STARTUP')], title: 'Encontrar Mentores | Ponti' },
      { path: 'mentores/:id/agendar', component: AgendarMentoriaComponent, canActivate: [tipoGuard('STARTUP')], title: 'Agendar mentoria | Ponti' },

      // Telas só de mentor
      { path: 'encontrar-startups', component: EncontrarStartupsComponent, canActivate: [tipoGuard('MENTOR')], title: 'Encontrar Startups | Ponti' },
      { path: 'agenda', component: MinhaAgendaComponent, canActivate: [tipoGuard('MENTOR')], title: 'Minha agenda | Ponti' },

      // Telas dos dois tipos
      { path: 'mentorias', component: MentoriasComponent, title: 'Mentorias | Ponti' },
      { path: 'convites', pathMatch: 'full', redirectTo: 'mentorias' },
      { path: 'mentores/:id/perfil', component: PerfilMentorComponent, title: 'Perfil do mentor | Ponti' },
      { path: 'startups/:id/perfil', component: PerfilStartupComponent, title: 'Perfil da startup | Ponti' },
      // /perfil mostra o próprio perfil; o botão "Editar perfil" leva para /perfil/editar
      { path: 'perfil', component: MeuPerfilComponent, title: 'Meu perfil | Ponti' },
      { path: 'perfil/editar', component: EditarPerfilComponent, title: 'Editar perfil | Ponti' },
      { path: 'configuracoes', component: ConfiguracoesComponent, title: 'Configurações | Ponti' },
    ],
  },
  { path: '**', redirectTo: '' },
];
