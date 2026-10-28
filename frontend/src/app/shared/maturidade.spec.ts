import { NIVEL_INICIAL, NIVEL_MAXIMO, NOMES_NIVEIS, PERGUNTAS_MATURIDADE, calcularNivel } from './maturidade';
import { descreverMaturidade, nomeDoNivel } from './maturidade.utils';

describe('calcularNivel', () => {
  it('sem respostas, a startup fica na ideia inicial (nível 1)', () => {
    expect(calcularNivel([])).toBe(NIVEL_INICIAL);
  });

  it('um "Não" logo na primeira pergunta mantém o nível 1', () => {
    expect(calcularNivel([false])).toBe(1);
  });

  it('cada "Sim" seguido sobe um nível', () => {
    expect(calcularNivel([true])).toBe(2);
    expect(calcularNivel([true, true])).toBe(3);
    expect(calcularNivel([true, true, true, true])).toBe(5);
  });

  it('o primeiro "Não" encerra: respostas depois dele são ignoradas', () => {
    expect(calcularNivel([true, true, false, true, true])).toBe(3);
  });

  it('todas as respostas "Sim" chegam ao nível 9', () => {
    expect(calcularNivel(PERGUNTAS_MATURIDADE.map(() => true))).toBe(NIVEL_MAXIMO);
  });

  it('respostas além da última pergunta não passam do nível 9', () => {
    expect(calcularNivel([...PERGUNTAS_MATURIDADE.map(() => true), true, true])).toBe(NIVEL_MAXIMO);
  });
});

describe('perguntas de maturidade', () => {
  it('cobrem os níveis 2 a 9, um por pergunta e em ordem', () => {
    expect(PERGUNTAS_MATURIDADE.map((p) => p.nivel)).toEqual([2, 3, 4, 5, 6, 7, 8, 9]);
  });

  it('todos os níveis de 1 a 9 têm nome', () => {
    for (let n = 1; n <= NIVEL_MAXIMO; n++) {
      expect(NOMES_NIVEIS[n]).toBeTruthy();
    }
  });
});

describe('descrição da maturidade', () => {
  it('é vazia quando a startup ainda não tem nível', () => {
    expect(descreverMaturidade(null)).toBe('');
    expect(descreverMaturidade(undefined)).toBe('');
    expect(nomeDoNivel(null)).toBe('');
  });

  it('mostra o nível e o nome', () => {
    expect(descreverMaturidade(4)).toBe('Nível 4 de 9 · Prova de conceito pronta');
    expect(nomeDoNivel(1)).toBe('Ideia inicial');
  });
});
