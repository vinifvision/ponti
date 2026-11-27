import { Routes, UrlMatcher } from '@angular/router';
import { DetalheAcompanhamentoComponent } from './detalhe/detalhe-acompanhamento.component';
import { AtividadesAcompanhamentoComponent } from './detalhe/atividades/atividades-acompanhamento.component';
import { ListaAcompanhamentosComponent } from './lista/lista-acompanhamentos.component';
import { ehFiltroAtividades } from './acompanhamento.utils';

// As quatro abas (todas, pendentes, atrasadas, devolvidas) são a MESMA tela com um filtro na URL:
// /acompanhamentos/:id/todas, .../pendentes, .../atrasadas e .../devolvidas. Assim a tela não é recriada
// nem recarrega as atividades ao trocar de aba. O valor chega ao componente como o input "filtro".
const abaDeAtividades: UrlMatcher = (segmentos) =>
  segmentos.length === 1 && ehFiltroAtividades(segmentos[0].path)
    ? { consumed: segmentos, posParams: { filtro: segmentos[0] } }
    : null;

// Rotas do módulo, carregadas sob demanda (só baixam quando a pessoa abre a aba Acompanhamentos).
export const ACOMPANHAMENTOS_ROUTES: Routes = [
  { path: '', component: ListaAcompanhamentosComponent, title: 'Acompanhamentos | Ponti' },
  {
    // Página do acompanhamento (Tela 3): casca com as subabas como rotas filhas
    path: ':id',
    component: DetalheAcompanhamentoComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'todas' },
      { matcher: abaDeAtividades, component: AtividadesAcompanhamentoComponent, title: 'Atividades | Ponti' },
      // Endereços das abas antigas (links salvos): levam para a aba "Todas atividades"
      { path: 'atividades', redirectTo: 'todas' },
      { path: 'visao-geral', redirectTo: 'todas' },
      { path: 'relatorios', redirectTo: 'todas' },
      { path: 'reunioes', redirectTo: 'todas' },
    ],
  },
];
