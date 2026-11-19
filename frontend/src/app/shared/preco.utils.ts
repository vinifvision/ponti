// Faixas do filtro "Faixa de preço" da busca de mentores (valorSessao, em reais, por sessão).
// O filtro é aplicado no servidor (BuscaMentores, no backend); aqui ficam só as opções da tela.
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
