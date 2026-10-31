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

// Um trecho do texto já separado para a tela montar: título, parágrafo ou lista.
export interface BlocoTexto {
  tipo: 'titulo' | 'paragrafo' | 'lista';
  texto: string;
  itens: string[];
}

// Transforma o texto digitado em blocos (sem HTML, então nada digitado vira código na tela):
//   "## Título"        -> título
//   "- item" / "* item" -> lista (linhas seguidas viram uma lista só)
//   linhas seguidas    -> parágrafo (mantém as quebras de linha); linha em branco separa os blocos
export function blocosDeTexto(texto: string | null | undefined): BlocoTexto[] {
  const blocos: BlocoTexto[] = [];
  let paragrafo: string[] = [];
  let itens: string[] = [];

  const fecharParagrafo = () => {
    if (paragrafo.length) blocos.push({ tipo: 'paragrafo', texto: paragrafo.join('\n'), itens: [] });
    paragrafo = [];
  };
  const fecharLista = () => {
    if (itens.length) blocos.push({ tipo: 'lista', texto: '', itens });
    itens = [];
  };

  for (const bruta of (texto ?? '').replace(/\r\n?/g, '\n').split('\n')) {
    const linha = bruta.trim();
    const titulo = linha.match(/^#{1,3}\s+(.+)$/);
    const item = linha.match(/^[-*•]\s+(.+)$/);

    if (!linha) {
      fecharParagrafo();
      fecharLista();
    } else if (titulo) {
      fecharParagrafo();
      fecharLista();
      blocos.push({ tipo: 'titulo', texto: titulo[1].trim(), itens: [] });
    } else if (item) {
      fecharParagrafo();
      itens.push(item[1].trim());
    } else {
      fecharLista();
      paragrafo.push(linha);
    }
  }
  fecharParagrafo();
  fecharLista();
  return blocos;
}
