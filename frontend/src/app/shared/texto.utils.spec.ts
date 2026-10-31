import { blocosDeTexto } from './texto.utils';

describe('blocosDeTexto', () => {
  it('devolve vazio para texto vazio ou nulo', () => {
    expect(blocosDeTexto('')).toEqual([]);
    expect(blocosDeTexto('   \n  ')).toEqual([]);
    expect(blocosDeTexto(null)).toEqual([]);
    expect(blocosDeTexto(undefined)).toEqual([]);
  });

  it('separa parágrafos por linha em branco e mantém as quebras de linha dentro do parágrafo', () => {
    const blocos = blocosDeTexto('Primeira linha\nsegunda linha\n\nOutro parágrafo');
    expect(blocos.map((b) => [b.tipo, b.texto])).toEqual([
      ['paragrafo', 'Primeira linha\nsegunda linha'],
      ['paragrafo', 'Outro parágrafo'],
    ]);
  });

  it('transforma "## Título" em título (de 1 a 3 #)', () => {
    const blocos = blocosDeTexto('# Um\n## Dois\n### Três\n#Sem espaço');
    expect(blocos.map((b) => [b.tipo, b.texto])).toEqual([
      ['titulo', 'Um'],
      ['titulo', 'Dois'],
      ['titulo', 'Três'],
      ['paragrafo', '#Sem espaço'],
    ]);
  });

  it('junta linhas com "-", "*" ou "•" em uma lista só', () => {
    const blocos = blocosDeTexto('- um\n* dois\n• três');
    expect(blocos).toEqual([{ tipo: 'lista', texto: '', itens: ['um', 'dois', 'três'] }]);
  });

  it('mistura parágrafo, lista e título sem precisar de linha em branco entre eles', () => {
    const blocos = blocosDeTexto('Resumo\n- a\n- b\n## Próximos passos\nTexto final');
    expect(blocos.map((b) => b.tipo)).toEqual(['paragrafo', 'lista', 'titulo', 'paragrafo']);
    expect(blocos[1].itens).toEqual(['a', 'b']);
  });

  it('não confunde "-5%" ou um traço sozinho com item de lista', () => {
    expect(blocosDeTexto('-5% de custo').map((b) => b.tipo)).toEqual(['paragrafo']);
    expect(blocosDeTexto('-').map((b) => b.tipo)).toEqual(['paragrafo']);
  });

  it('aceita quebras de linha do Windows', () => {
    expect(blocosDeTexto('a\r\n\r\nb').map((b) => b.texto)).toEqual(['a', 'b']);
  });

  it('mantém HTML digitado como texto puro (não vira código)', () => {
    expect(blocosDeTexto('<b>oi</b>')[0].texto).toBe('<b>oi</b>');
  });
});
