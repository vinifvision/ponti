// Faixas do filtro "Experiência" da busca de mentores (o backend guarda anosExperiencia como número inteiro).
//   iniciante:     até 2 anos
//   intermediario: de 3 a 5 anos
//   senior:        mais de 5 anos
export function atendeFaixaExperiencia(anos: number | null | undefined, faixa: string): boolean {
  if (!faixa) return true; // sem filtro escolhido, todo mundo passa
  if (anos === null || anos === undefined) return false; // mentor sem a informação não entra numa faixa

  switch (faixa) {
    case 'iniciante':
      return anos <= 2;
    case 'intermediario':
      return anos > 2 && anos <= 5;
    case 'senior':
      return anos > 5;
    default:
      return true;
  }
}
