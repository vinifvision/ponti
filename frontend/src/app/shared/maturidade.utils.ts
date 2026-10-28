import { NIVEL_MAXIMO, NOMES_NIVEIS } from './maturidade';

// "Nível 4 de 9 · Prova de conceito pronta", ou '' se a startup ainda não respondeu o questionário
export function descreverMaturidade(nivel: number | null | undefined): string {
  if (!nivel) return '';
  return `Nível ${nivel} de ${NIVEL_MAXIMO} · ${NOMES_NIVEIS[nivel] ?? ''}`.trim();
}

export function nomeDoNivel(nivel: number | null | undefined): string {
  return nivel ? (NOMES_NIVEIS[nivel] ?? '') : '';
}

export { NIVEL_MAXIMO };
