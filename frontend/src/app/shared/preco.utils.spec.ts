import { FAIXAS_PRECO, atendeFaixaPreco } from './preco.utils';

describe('atendeFaixaPreco', () => {
  it('sem faixa escolhida, todo mundo passa (até quem não tem valor definido)', () => {
    expect(atendeFaixaPreco(120, '')).toBeTrue();
    expect(atendeFaixaPreco(0, '')).toBeTrue();
    expect(atendeFaixaPreco(null, '')).toBeTrue();
    expect(atendeFaixaPreco(undefined, '')).toBeTrue();
  });

  it('com faixa escolhida, mentor sem valor definido ("A combinar") não passa', () => {
    for (const faixa of FAIXAS_PRECO) {
      expect(atendeFaixaPreco(null, faixa.valor)).withContext(faixa.rotulo).toBeFalse();
      expect(atendeFaixaPreco(undefined, faixa.valor)).withContext(faixa.rotulo).toBeFalse();
    }
  });

  it('gratuita: só o valor 0', () => {
    expect(atendeFaixaPreco(0, 'gratuita')).toBeTrue();
    expect(atendeFaixaPreco(0.01, 'gratuita')).toBeFalse();
    expect(atendeFaixaPreco(50, 'gratuita')).toBeFalse();
  });

  it('o mentor gratuito não aparece nas faixas pagas', () => {
    expect(atendeFaixaPreco(0, 'ate-100')).toBeFalse();
    expect(atendeFaixaPreco(0, '100-200')).toBeFalse();
    expect(atendeFaixaPreco(0, 'acima-200')).toBeFalse();
  });

  it('até R$ 100: acima de zero, incluindo o próprio 100', () => {
    expect(atendeFaixaPreco(0.01, 'ate-100')).toBeTrue();
    expect(atendeFaixaPreco(99.9, 'ate-100')).toBeTrue();
    expect(atendeFaixaPreco(100, 'ate-100')).toBeTrue();
    expect(atendeFaixaPreco(100.01, 'ate-100')).toBeFalse();
  });

  it('R$ 100 a R$ 200: acima de 100, incluindo o próprio 200', () => {
    expect(atendeFaixaPreco(100, '100-200')).toBeFalse();
    expect(atendeFaixaPreco(120, '100-200')).toBeTrue();
    expect(atendeFaixaPreco(200, '100-200')).toBeTrue();
    expect(atendeFaixaPreco(200.01, '100-200')).toBeFalse();
  });

  it('acima de R$ 200', () => {
    expect(atendeFaixaPreco(200, 'acima-200')).toBeFalse();
    expect(atendeFaixaPreco(200.01, 'acima-200')).toBeTrue();
    expect(atendeFaixaPreco(100000, 'acima-200')).toBeTrue();
  });

  it('cada valor definido cai em exatamente uma faixa', () => {
    for (const valor of [0, 0.01, 1, 50, 99.99, 100, 100.01, 150, 199.99, 200, 200.01, 500, 100000]) {
      const faixas = FAIXAS_PRECO.filter((f) => atendeFaixaPreco(valor, f.valor));
      expect(faixas.length).withContext(`R$ ${valor}`).toBe(1);
    }
  });
});
