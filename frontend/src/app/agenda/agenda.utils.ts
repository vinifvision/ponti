import { DiaSemana, FaixaHorario } from '../models';
import { lerDataHora } from '../shared/data.utils';

// Os horários começam em hora cheia ou meia hora (09:00, 09:30), para os blocos da sessão ficarem alinhados.
export const PASSO_MINUTOS = 30;

const DIAS_DA_SEMANA: DiaSemana[] = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

// "2026-10-29" -> 'THURSDAY'
export function diaSemanaDe(iso: string): DiaSemana {
  return DIAS_DA_SEMANA[lerDataHora(iso).getDay()];
}

// "13:30" -> 810 (minutos desde 00:00); texto inválido vira NaN
export function minutosDoDia(horario: string): number {
  const achado = /^(\d{1,2}):(\d{2})/.exec(horario ?? '');
  if (!achado) return NaN;
  return Number(achado[1]) * 60 + Number(achado[2]);
}

// Confere uma lista de faixas do mesmo dia. Devolve a mensagem do primeiro problema, ou '' se estiver tudo certo.
// duracaoMinutos: duração da sessão do mentor; a faixa precisa comportar pelo menos uma sessão.
export function validarFaixas(faixas: FaixaHorario[], duracaoMinutos = 60): string {
  const lista: { inicio: number; fim: number }[] = [];

  for (const f of faixas) {
    const inicio = minutosDoDia(f.horaInicio);
    const fim = minutosDoDia(f.horaFim);
    if (Number.isNaN(inicio) || Number.isNaN(fim)) return 'Preencha o início e o término de todos os horários.';
    if (fim <= inicio) return 'O horário de término precisa ser depois do início.';
    if (inicio % PASSO_MINUTOS !== 0 || fim % PASSO_MINUTOS !== 0) {
      return 'Use horários em hora cheia ou meia hora (por exemplo, 09:00 ou 09:30).';
    }
    if (fim - inicio < duracaoMinutos) {
      return `Cada faixa precisa ter pelo menos ${duracaoMinutos} minutos, o tempo de uma sessão.`;
    }
    lista.push({ inicio, fim });
  }

  lista.sort((a, b) => a.inicio - b.inicio);
  for (let i = 1; i < lista.length; i++) {
    // Faixas encostadas (12:00 às 14:00 e 14:00 às 16:00) são permitidas
    if (lista[i].inicio < lista[i - 1].fim) return 'Há horários sobrepostos no mesmo dia.';
  }
  return '';
}
