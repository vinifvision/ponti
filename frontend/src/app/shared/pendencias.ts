import type { Agendamento, Convite } from '../models';

// Mentor: pedidos de mentoria que a startup fez e ele ainda não confirmou nem recusou.
export function contarPedidosPendentes(agendamentos: Pick<Agendamento, 'status'>[]): number {
  return agendamentos.filter((a) => a.status === 'PENDENTE').length;
}

// Startup: mentorias sugeridas por mentores que ainda não foram agendadas.
export function contarSugestoesPendentes(convites: Pick<Convite, 'status' | 'enviadoPor'>[]): number {
  return convites.filter((c) => c.enviadoPor === 'MENTOR' && c.status === 'PENDENTE').length;
}

export function descreverPendencias(quantidade: number): string {
  if (quantidade <= 0) return 'Notificações: nada pendente';
  return quantidade === 1 ? 'Notificações: 1 pendência' : `Notificações: ${quantidade} pendências`;
}
