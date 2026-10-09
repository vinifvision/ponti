export function iniciais(nome: string): string {
  return nome
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join('');
}

export function separarLista(valor?: string): string[] {
  return (valor ?? '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}
