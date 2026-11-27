import { Dashboard, Etapa } from './admin.models';

// Regras de texto e de conta do painel do admin, sem Angular (fáceis de testar em admin.utils.spec.ts)

export interface Variacao {
  texto: string;
  sentido: 'subiu' | 'caiu' | 'igual';
}

// Comparação com o período anterior: "+29%", "-15%", "igual" ou "+3" quando antes era zero
export function variacao(atual: number, anterior: number): Variacao {
  if (atual === anterior) return { texto: 'igual', sentido: 'igual' };
  const sentido = atual > anterior ? 'subiu' : 'caiu';
  if (anterior === 0) return { texto: `+${atual}`, sentido };
  const pct = Math.round(((atual - anterior) / anterior) * 100);
  return { texto: `${pct > 0 ? '+' : ''}${pct}%`, sentido };
}

// Largura de uma barra em % do maior valor (nunca menor que 2% quando há valor, para a barra não sumir)
export function largura(valor: number, maximo: number): number {
  if (!maximo || valor <= 0) return 0;
  return Math.max(2, Math.round((valor / maximo) * 100));
}

// Os períodos rápidos do filtro
export const PERIODOS = [
  { dias: 7, rotulo: '7 dias' },
  { dias: 30, rotulo: '30 dias' },
  { dias: 90, rotulo: '90 dias' },
] as const;

// "2026-11-27" (no horário local do navegador)
export function iso(d: Date): string {
  const dois = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())}`;
}

// Os últimos N dias, contando hoje
export function ultimosDias(dias: number, hoje = new Date()): { inicio: string; fim: string } {
  const inicio = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - (dias - 1));
  return { inicio: iso(inicio), fim: iso(hoje) };
}

// "2026-11-27" -> "27 nov"
export function dataCurta(texto: string): string {
  const [ano, mes, dia] = texto.split('-').map(Number);
  return new Date(ano, mes - 1, dia).toLocaleDateString('pt-BR', { day: 'numeric', month: 'short' }).replace('.', '');
}

// Instante da API ("2026-11-27T13:05:00Z") -> "27/11/2026"
export function dataDoInstante(texto: string | null): string {
  if (!texto) return 'Sem data';
  return new Date(texto).toLocaleDateString('pt-BR');
}

// Instante -> "27/11/2026 às 10:05"
export function dataHoraDoInstante(texto: string): string {
  const d = new Date(texto);
  const hora = d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  return `${d.toLocaleDateString('pt-BR')} às ${hora}`;
}

// Iniciais para o avatar: "Feira Já" -> "FJ"
export function iniciais(nome: string | null, email: string): string {
  const base = (nome?.trim() || email).split(/[\s@.]+/).filter(Boolean);
  return ((base[0]?.[0] ?? '') + (base[1]?.[0] ?? '')).toUpperCase() || '?';
}

// Nomes das etapas do funil, pelo que a startup fez
export const ROTULO_ETAPA: Record<Etapa, string> = {
  CADASTRO: 'Se cadastraram',
  INTERESSE: 'Pediram mentoria',
  MATCH: 'Foram aceitas por um mentor',
  MENTORIA: 'Tiveram a mentoria',
  ACOMPANHAMENTO: 'Estão em acompanhamento',
};

// Nomes dos números de destaque
export const ROTULO_DESTAQUE: Partial<Record<string, string>> = {
  startups: 'Startups cadastradas',
  mentores: 'Mentores cadastrados',
  matches: 'Pedidos aceitos',
  mentorias: 'Mentorias realizadas',
};

// O que aconteceu com quem parou antes de cada etapa, e o que fazer (frases fixas, escolhidas pela etapa
// onde mais startups pararam; não é inteligência artificial)
const PERDA_NO_FUNIL: Record<Etapa, { parou: [string, string]; acao: string }> = {
  CADASTRO: { parou: ['', ''], acao: '' },
  INTERESSE: {
    parou: ['se cadastrou e não pediu nenhuma mentoria', 'se cadastraram e não pediram nenhuma mentoria'],
    acao: 'mostrar os mentores recomendados logo depois do cadastro.',
  },
  MATCH: {
    parou: ['pediu mentoria e ainda não foi aceita por nenhum mentor', 'pediram mentoria e ainda não foram aceitas por nenhum mentor'],
    acao: 'lembrar os mentores de responder e indicar mentores mais compatíveis.',
  },
  MENTORIA: {
    parou: ['foi aceita e ainda não teve a mentoria', 'foram aceitas e ainda não tiveram a mentoria'],
    acao: 'lembrar as duas partes das sessões marcadas.',
  },
  ACOMPANHAMENTO: {
    parou: ['teve a mentoria e não seguiu para um acompanhamento', 'tiveram a mentoria e não seguiram para um acompanhamento'],
    acao: 'incentivar o mentor a propor um acompanhamento depois da sessão.',
  },
};

// A frase de cada bloco: o que o número mostra e, quando há, o que fazer
export interface Decisao {
  texto: string;
  acao: string | null;
}

function plural(n: number, singular: string, pluralTexto: string): string {
  return `${n} ${n === 1 ? singular : pluralTexto}`;
}

export function decisoes(d: Dashboard): Record<'funil' | 'match' | 'oferta' | 'saude' | 'atencao', Decisao> {
  const etapas = d.funil.etapas;
  const k = etapas.findIndex((e) => e.etapa === d.funil.maiorQueda);
  let funil: Decisao;
  if (k > 0) {
    const perda = etapas[k - 1].quantidade - etapas[k].quantidade;
    const p = PERDA_NO_FUNIL[etapas[k].etapa];
    funil = { texto: `${plural(perda, 'startup', 'startups')} ${p.parou[perda === 1 ? 0 : 1]}.`, acao: p.acao };
  } else {
    funil = { texto: 'Nenhuma startup se cadastrou neste período.', acao: null };
  }

  const alta = d.match.faixas.find((f) => f.faixa === 'ALTA');
  const baixa = d.match.faixas.find((f) => f.faixa === 'BAIXA');
  let match: Decisao;
  if (d.match.diferenca === null || !alta || !baixa) {
    match = { texto: 'Ainda não há pedidos suficientes neste período para comparar.', acao: null };
  } else if (d.match.diferenca >= 30) {
    match = {
      texto: `Com compatibilidade alta, ${alta.taxa}% dos pedidos foram aceitos. Com compatibilidade baixa, só ${baixa.taxa}%. A recomendação da Ponti funciona.`,
      acao: null,
    };
  } else {
    match = {
      texto: `A diferença entre compatibilidade alta e baixa é de só ${d.match.diferenca} pontos.`,
      acao: 'revisar os pesos do cálculo de compatibilidade.',
    };
  }

  const faltam = d.ofertaDemanda.filter((t) => t.situacao === 'FALTA').map((t) => t.rotulo);
  const oferta: Decisao = faltam.length
    ? { texto: `Faltam mentores em ${juntar(faltam)}.`, acao: 'priorizar esses temas ao recrutar mentores.' }
    : { texto: 'Oferta e procura estão equilibradas em todos os temas.', acao: null };

  const n = d.saude.parados.length;
  const saude: Decisao = n
    ? {
        texto: `${plural(n, 'acompanhamento está', 'acompanhamentos estão')} sem movimento há mais de 14 dias.`,
        acao: n === 1 ? 'falar com o mentor.' : 'falar com os mentores.',
      }
    : { texto: 'Todos os acompanhamentos ativos tiveram movimento nas últimas 2 semanas.', acao: null };

  const a = d.atencao.length;
  const atencao: Decisao = {
    texto: a ? `${plural(a, 'conta', 'contas')} para olhar de perto.` : 'Nenhuma conta precisa de atenção agora.',
    acao: null,
  };

  return { funil, match, oferta, saude, atencao };
}

// ["A", "B", "C"] -> "A, B e C"
export function juntar(itens: string[]): string {
  if (itens.length <= 1) return itens.join('');
  return `${itens.slice(0, -1).join(', ')} e ${itens[itens.length - 1]}`;
}
