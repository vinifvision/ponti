import { fotografar } from './mudancas';

describe('fotografar', () => {
  it('duas fotos do mesmo conteúdo são iguais', () => {
    const base = { nome: 'Ana', temas: ['Pitch'] };
    expect(fotografar({ ...base, temas: [...base.temas] })).toBe(fotografar(base));
  });

  it('muda quando um campo muda', () => {
    expect(fotografar({ nome: 'Ana' })).not.toBe(fotografar({ nome: 'Ana Maria' }));
  });

  it('muda quando um item é adicionado ou removido de uma lista', () => {
    expect(fotografar({ temas: ['Pitch'] })).not.toBe(fotografar({ temas: ['Pitch', 'Vendas'] }));
    expect(fotografar({ temas: ['Pitch'] })).not.toBe(fotografar({ temas: [] }));
  });

  it('campo vazio, nulo ou ausente é a mesma coisa (digitar e apagar não é mudança)', () => {
    expect(fotografar({ nome: 'Ana', bio: '' })).toBe(fotografar({ nome: 'Ana' }));
    expect(fotografar({ nome: 'Ana', bio: null })).toBe(fotografar({ nome: 'Ana' }));
  });

  it('o número zero é um valor de verdade (diferente de vazio)', () => {
    expect(fotografar({ valor: 0 })).not.toBe(fotografar({}));
  });
});
