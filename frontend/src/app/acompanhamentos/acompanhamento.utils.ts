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

// "Atrasada" não é status: vale quando o prazo passou e a startup ainda precisa entregar (ou reenviar)
export function estaAtrasada(a: Atividade, agora: Date = new Date()): boolean {
  return (a.status === 'PENDENTE' || a.status === 'DEVOLVIDA') && lerDataHora(a.prazo) < agora;
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
