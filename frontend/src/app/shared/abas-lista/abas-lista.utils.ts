export interface AbaLista {
  chave: string;
  rotulo: string;
  // Mostra a bolinha de aviso ao lado do rótulo
  notificacao?: boolean;
}

// Qual aba abrir: a pedida na URL (?aba=...) se ela existir; senão a primeira que tem algo para mostrar
// (assim ninguém cai numa aba vazia); se todas estiverem vazias, a primeira.
export function escolherAba<T extends string>(pedida: string | null | undefined, abas: { chave: T; quantidade: number }[]): T {
  const pedidaValida = abas.find((a) => a.chave === pedida);
  if (pedidaValida) return pedidaValida.chave;
  return (abas.find((a) => a.quantidade > 0) ?? abas[0]).chave;
}
