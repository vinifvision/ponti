import { lerDataHora } from '../shared/data.utils';
import { TomSelo } from '../shared/selo/selo.component';
import { Atividade, Entrega, ResumoAcompanhamento } from './acompanhamento.models';

export interface SeloCard {
  texto: string;
  tom: TomSelo;
}

export interface GruposAcompanhamento {
  pendentes: ResumoAcompanhamento[];
  ativos: ResumoAcompanhamento[];
  encerrados: ResumoAcompanhamento[];
}

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

// Porcentagem de 0 a 100 (0 quando ainda não há nada a medir)
export function percentual(parte: number, total: number): number {
  if (total <= 0) return 0;
  return Math.min(100, Math.max(0, Math.round((parte / total) * 100)));
}

export function plural(n: number, singular: string, mais: string): string {
  return `${n} ${n === 1 ? singular : mais}`;
}

// "2026-11-12" -> "12 nov" (sem passar por Date, para o fuso horário não mudar o dia)
export function dataCurta(iso: string | null | undefined): string {
  if (!iso) return '';
  const [, mes, dia] = iso.slice(0, 10).split('-');
  const nome = MESES[Number(mes) - 1];
  return nome && dia ? `${dia} ${nome}` : '';
}

// Separa a lista nos três grupos da tela. Propostas recusadas não aparecem.
// Ativos: quem precisa de mais atenção (atrasos, depois pendências) vem primeiro. Encerrados: os mais recentes primeiro.
export function agruparPorStatus(lista: ResumoAcompanhamento[]): GruposAcompanhamento {
  const atencao = (a: ResumoAcompanhamento) => a.atividadesAtrasadas * 1000 + a.pendencias;
  return {
    pendentes: lista.filter((a) => a.status === 'PROPOSTO'),
    ativos: lista.filter((a) => a.status === 'ATIVO').sort((a, b) => atencao(b) - atencao(a)),
    encerrados: lista
      .filter((a) => a.status === 'ENCERRADO')
      .sort((a, b) => (b.dataFim ?? '').localeCompare(a.dataFim ?? '')),
  };
}

// ---------- Abas da lista de acompanhamentos ----------

export type AbaListaAcompanhamento = 'pendentes' | 'ativos' | 'encerrados';

// Rótulo e chave de cada aba, na ordem em que aparecem
export const ABAS_LISTA_ACOMPANHAMENTOS: { chave: AbaListaAcompanhamento; rotulo: string }[] = [
  { chave: 'ativos', rotulo: 'Ativos' },
  { chave: 'pendentes', rotulo: 'Propostas pendentes' },
  { chave: 'encerrados', rotulo: 'Encerrados' },
];

// Quais abas ganham a bolinha de aviso (só o que espera uma ação da pessoa):
//   pendentes:  só para a startup, que precisa aceitar ou recusar. Para o mentor a proposta espera a startup.
//   ativos:     algum acompanhamento com atividade atrasada ou pendência (startup: a entregar; mentor: a revisar).
//   encerrados: nunca (somente leitura).
export function notificacoesDaLista(grupos: GruposAcompanhamento, ehMentor: boolean): Record<AbaListaAcompanhamento, boolean> {
  return {
    pendentes: !ehMentor && grupos.pendentes.length > 0,
    ativos: grupos.ativos.some((a) => a.atividadesAtrasadas > 0 || a.pendencias > 0),
    encerrados: false,
  };
}

// Selos de situação que aparecem no topo do card
export function selosDoCard(a: ResumoAcompanhamento, ehMentor: boolean): SeloCard[] {
  switch (a.status) {
    case 'PROPOSTO':
      return ehMentor ? [{ texto: 'Aguardando resposta da startup', tom: 'aviso' }] : [];
    case 'ENCERRADO':
      return [{ texto: 'Somente leitura', tom: 'neutro' }];
    case 'ATIVO': {
      const selos: SeloCard[] = [];
      if (a.atividadesAtrasadas > 0) {
        selos.push({ texto: plural(a.atividadesAtrasadas, 'atividade atrasada', 'atividades atrasadas'), tom: 'perigo' });
      }
      if (a.pendencias > 0) {
        const texto = ehMentor
          ? plural(a.pendencias, 'entrega para revisar', 'entregas para revisar')
          : plural(a.pendencias, 'atividade para entregar', 'atividades para entregar');
        selos.push({ texto, tom: 'destaque' });
      }
      return selos.length ? selos : [{ texto: 'Em dia', tom: 'sucesso' }];
    }
    default:
      return [];
  }
}

// ---------- Atividades e linha do tempo ----------

export type TomNo = 'aprovada' | 'atrasada' | 'devolvida' | 'entregue' | 'pendente';

// Usa o campo `atrasada` calculado pelo servidor quando presente; caso contrário calcula pelo prazo (mock/testes).
export function estaAtrasada(a: Atividade, agora?: Date): boolean {
  if (agora === undefined && typeof a.atrasada === 'boolean') return a.atrasada;
  const ref = agora ?? new Date();
  return (a.status === 'PENDENTE' || a.status === 'DEVOLVIDA') && lerDataHora(a.prazo) < ref;
}

export function diasDeAtraso(a: Atividade, agora: Date = new Date()): number {
  const ms = agora.getTime() - lerDataHora(a.prazo).getTime();
  return Math.max(0, Math.floor(ms / 86_400_000));
}

export function entregueComAtraso(a: Atividade, e: Entrega): boolean {
  return lerDataHora(e.enviadaEm) > lerDataHora(a.prazo);
}

function horaMinuto(iso: string): string {
  return (iso.split('T')[1] ?? '00:00').slice(0, 5);
}

// "2026-11-19T15:30:00" -> "19 nov às 15:30"
export function dataHoraCurta(iso: string): string {
  return `${dataCurta(iso)} às ${horaMinuto(iso)}`;
}

// "2026-11-19T23:59:00" -> "19 nov, 23:59"
export function prazoCurto(iso: string): string {
  return `${dataCurta(iso)}, ${horaMinuto(iso)}`;
}

export function textoPrazo(a: Atividade, agora: Date = new Date()): string {
  const prazo = prazoCurto(a.prazo);
  if (estaAtrasada(a, agora)) {
    const dias = diasDeAtraso(a, agora);
    return dias >= 1 ? `Venceu em ${prazo} (há ${plural(dias, 'dia', 'dias')})` : `Venceu em ${prazo}`;
  }
  return a.status === 'PENDENTE' || a.status === 'DEVOLVIDA' ? `Entregar até ${prazo}` : `Prazo: ${prazo}`;
}

export function selosDaAtividade(a: Atividade, agora: Date = new Date()): SeloCard[] {
  const base: Record<Atividade['status'], SeloCard> = {
    PENDENTE: { texto: 'Pendente', tom: 'neutro' },
    ENTREGUE: { texto: 'Aguardando revisão', tom: 'destaque' },
    APROVADA: { texto: 'Aprovada', tom: 'sucesso' },
    DEVOLVIDA: { texto: 'Devolvida para ajustes', tom: 'aviso' },
  };
  const selos = [base[a.status]];
  if (estaAtrasada(a, agora)) selos.push({ texto: 'Atrasada', tom: 'perigo' });
  return selos;
}

// Aparência da bolinha numerada da linha do tempo
export function tomDoNo(a: Atividade, agora: Date = new Date()): TomNo {
  if (a.status === 'APROVADA') return 'aprovada';
  if (estaAtrasada(a, agora)) return 'atrasada';
  if (a.status === 'DEVOLVIDA') return 'devolvida';
  if (a.status === 'ENTREGUE') return 'entregue';
  return 'pendente';
}

// A linha do tempo vai do prazo mais próximo para o mais distante
export function ordenarPorPrazo(lista: Atividade[]): Atividade[] {
  return [...lista].sort((a, b) => a.prazo.localeCompare(b.prazo));
}

// ---------- Abas da página do acompanhamento ----------

export type FiltroAtividades = 'todas' | 'pendentes' | 'atrasadas' | 'devolvidas';

// Rótulo e endereço de cada aba, na ordem em que aparecem
export const ABAS_ATIVIDADES: { rotulo: string; rota: FiltroAtividades }[] = [
  { rotulo: 'Todas atividades', rota: 'todas' },
  { rotulo: 'Pendentes', rota: 'pendentes' },
  { rotulo: 'Atrasadas', rota: 'atrasadas' },
  { rotulo: 'Devolvidas', rota: 'devolvidas' },
];

export function ehFiltroAtividades(valor: string): valor is FiltroAtividades {
  return ABAS_ATIVIDADES.some((aba) => aba.rota === valor);
}

// Atividades de cada aba, sempre do prazo mais próximo ao mais distante:
//   todas:      todas as atividades (a única aba que mostra a linha do tempo)
//   pendentes:  ainda não entregues (status PENDENTE)
//   atrasadas:  prazo vencido e a startup ainda precisa entregar ou reenviar (mesma regra do selo "Atrasada")
//   devolvidas: o mentor devolveu para ajustes (status DEVOLVIDA)
// Uma atividade pode estar em duas abas (ex.: pendente e atrasada).
export function filtrarAtividades(lista: Atividade[], filtro: FiltroAtividades, agora: Date = new Date()): Atividade[] {
  const ordenadas = ordenarPorPrazo(lista);
  switch (filtro) {
    case 'pendentes':
      return ordenadas.filter((a) => a.status === 'PENDENTE');
    case 'atrasadas':
      return ordenadas.filter((a) => estaAtrasada(a, agora));
    case 'devolvidas':
      return ordenadas.filter((a) => a.status === 'DEVOLVIDA');
    default:
      return ordenadas;
  }
}

// Quais abas ganham a bolinha de aviso. Só o que a pessoa ainda pode resolver; acompanhamento encerrado
// (ou proposta sem aceite) é somente leitura e não mostra nenhuma bolinha.
//   todas:      mentor, quando há entregas aguardando revisão (elas só aparecem nesta aba)
//   pendentes:  startup, quando há atividade ainda não entregue
//   atrasadas:  os dois lados, quando há atividade com prazo vencido
//   devolvidas: startup, quando o mentor devolveu algo para ajustar
export function notificacoesDasAbas(
  lista: Atividade[],
  ehMentor: boolean,
  somenteLeitura: boolean,
  agora?: Date,
): Record<FiltroAtividades, boolean> {
  if (somenteLeitura) return { todas: false, pendentes: false, atrasadas: false, devolvidas: false };
  return {
    todas: ehMentor && lista.some((a) => a.status === 'ENTREGUE'),
    pendentes: !ehMentor && lista.some((a) => a.status === 'PENDENTE'),
    atrasadas: lista.some((a) => estaAtrasada(a, agora)),
    devolvidas: !ehMentor && lista.some((a) => a.status === 'DEVOLVIDA'),
  };
}

// Números do card da lista, calculados a partir das atividades
export function resumirAtividades(
  lista: Atividade[],
  ehMentor: boolean,
  agora: Date = new Date(),
): Pick<ResumoAcompanhamento, 'atividadesAprovadas' | 'atividadesTotal' | 'atividadesAtrasadas' | 'pendencias' | 'proximoPrazo'> {
  const aEntregar = lista.filter((a) => a.status === 'PENDENTE' || a.status === 'DEVOLVIDA');
  const proximas = ordenarPorPrazo(aEntregar).filter((a) => lerDataHora(a.prazo) >= agora);
  return {
    atividadesAprovadas: lista.filter((a) => a.status === 'APROVADA').length,
    atividadesTotal: lista.length,
    atividadesAtrasadas: lista.filter((a) => estaAtrasada(a, agora)).length,
    pendencias: ehMentor ? lista.filter((a) => a.status === 'ENTREGUE').length : aEntregar.length,
    proximoPrazo: proximas.length ? proximas[0].prazo.slice(0, 10) : null,
  };
}
