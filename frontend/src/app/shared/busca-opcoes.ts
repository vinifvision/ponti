import { FAIXAS_PRECO } from './preco.utils';
import type { OpcaoFiltro } from './filtro-select/filtro-select.component';
import type { Startup } from '../models';

// Opções dos filtros de busca. Ficam aqui porque a navbar (painel de filtros da busca)
// e as telas "Encontrar mentores" / "Encontrar startups" mostram as mesmas listas.

export const OPCOES_EXPERIENCIA: OpcaoFiltro[] = [
  { valor: 'iniciante', rotulo: 'Até 2 anos' },
  { valor: 'intermediario', rotulo: '3 a 5 anos' },
  { valor: 'senior', rotulo: 'Mais de 5 anos' },
];

export const OPCOES_PRECO: OpcaoFiltro[] = FAIXAS_PRECO;

// "Recomendados" é a ordem padrão do servidor: a compatibilidade pesa mais, mas agenda livre, reputação e
// responsividade também contam. A porcentagem de compatibilidade continua no card.
export const ROTULO_RECOMENDADOS = 'Recomendados';

export const OPCOES_ORDENACAO_MENTORES: OpcaoFiltro[] = [
  { valor: 'recomendados', rotulo: ROTULO_RECOMENDADOS },
  { valor: 'nome', rotulo: 'Nome (A-Z)' },
  { valor: 'menor-valor', rotulo: 'Menor valor por sessão' },
  { valor: 'maior-valor', rotulo: 'Maior valor por sessão' },
];

export const OPCOES_MATURIDADE: OpcaoFiltro[] = [
  { valor: 'ideacao', rotulo: 'Ideação (níveis 1 a 3)' },
  { valor: 'validacao', rotulo: 'Validação (níveis 4 a 6)' },
  { valor: 'operacao', rotulo: 'Operação (níveis 7 a 9)' },
];

export const OPCOES_ORDENACAO_STARTUPS: OpcaoFiltro[] = [
  { valor: 'compatibilidade', rotulo: 'Mais compatíveis' },
  { valor: 'nome', rotulo: 'Nome (A-Z)' },
  { valor: 'mais-madura', rotulo: 'Mais madura primeiro' },
  { valor: 'menos-madura', rotulo: 'Menos madura primeiro' },
];

// Segmentos que aparecem no filtro "Segmento" das startups
export function opcoesDeSegmento(startups: Pick<Startup, 'segmento'>[]): OpcaoFiltro[] {
  const segmentos = new Set(startups.map((s) => s.segmento?.trim()).filter((s): s is string => !!s));
  return [...segmentos].sort().map((s) => ({ valor: s, rotulo: s }));
}
