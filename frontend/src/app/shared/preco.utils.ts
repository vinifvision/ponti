// Faixas do filtro "Faixa de preço" da busca de mentores (valorSessao, em reais, por sessão).
// "Gratuita" é uma faixa só dela: serve para mentores que estão começando e ainda não cobram.
// As faixas pagas começam acima de R$ 0, então um mentor gratuito aparece apenas em "Gratuita".
//   gratuita:   R$ 0
//   ate-100:    mais de R$ 0 até R$ 100
//   100-200:    mais de R$ 100 até R$ 200
//   acima-200:  mais de R$ 200
export const FAIXAS_PRECO: { valor: string; rotulo: string }[] = [
  { valor: 'gratuita', rotulo: 'Gratuita' },
  { valor: 'ate-100', rotulo: 'Até R$ 100' },
  { valor: '100-200', rotulo: 'R$ 100 a R$ 200' },
  { valor: 'acima-200', rotulo: 'Acima de R$ 200' },
];

export function atendeFaixaPreco(valorSessao: number | null | undefined, faixa: string): boolean {
  if (!faixa) return true; // sem faixa escolhida, todo mundo passa
  // Sem valor definido o card mostra "A combinar": não entra em nenhuma faixa de preço
  if (valorSessao === null || valorSessao === undefined) return false;

  const valor = Number(valorSessao);
  if (Number.isNaN(valor)) return false;

  switch (faixa) {
    case 'gratuita':
      return valor === 0;
    case 'ate-100':
      return valor > 0 && valor <= 100;
    case '100-200':
      return valor > 100 && valor <= 200;
    case 'acima-200':
      return valor > 200;
    default:
      return true;
  }
}
