import { Routes } from '@angular/router';
import { EmBreveComponent } from '../pages/em-breve/em-breve.component';
import { DetalheAcompanhamentoComponent } from './detalhe/detalhe-acompanhamento.component';
import { AtividadesAcompanhamentoComponent } from './detalhe/atividades/atividades-acompanhamento.component';
import { ListaAcompanhamentosComponent } from './lista/lista-acompanhamentos.component';

// Rotas do módulo, carregadas sob demanda (só baixam quando a pessoa abre a aba Acompanhamentos).
export const ACOMPANHAMENTOS_ROUTES: Routes = [
  { path: '', component: ListaAcompanhamentosComponent, title: 'Acompanhamentos | Ponti' },
  {
    // Página do acompanhamento (Tela 3): casca com as subabas como rotas filhas
    path: ':id',
    component: DetalheAcompanhamentoComponent,
    children: [
      // Enquanto a Visão geral não existe, a página abre direto nas Atividades
      { path: '', pathMatch: 'full', redirectTo: 'atividades' },
      { path: 'visao-geral', component: EmBreveComponent, data: { titulo: 'Visão geral' }, title: 'Visão geral | Ponti' },
      { path: 'atividades', component: AtividadesAcompanhamentoComponent, title: 'Atividades | Ponti' },
      { path: 'relatorios', component: EmBreveComponent, data: { titulo: 'Relatórios' }, title: 'Relatórios | Ponti' },
      { path: 'reunioes', component: EmBreveComponent, data: { titulo: 'Reuniões' }, title: 'Reuniões | Ponti' },
    ],
  },
];
