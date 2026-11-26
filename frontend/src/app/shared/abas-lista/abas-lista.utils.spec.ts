import { escolherAba } from './abas-lista.utils';

const abas = (p: number, a: number, e: number) => [
  { chave: 'pendentes' as const, quantidade: p },
  { chave: 'ativos' as const, quantidade: a },
  { chave: 'encerrados' as const, quantidade: e },
];

describe('escolherAba', () => {
  it('usa a aba pedida na URL, mesmo vazia', () => {
    expect(escolherAba('encerrados', abas(2, 1, 0))).toBe('encerrados');
  });

  it('sem pedido (ou com pedido inválido), abre a primeira aba que tem itens', () => {
    expect(escolherAba(undefined, abas(0, 3, 1))).toBe('ativos');
    expect(escolherAba('lixo', abas(0, 0, 4))).toBe('encerrados');
    expect(escolherAba(null, abas(1, 1, 1))).toBe('pendentes');
  });

  it('com tudo vazio, abre a primeira aba', () => {
    expect(escolherAba(undefined, abas(0, 0, 0))).toBe('pendentes');
  });
});
