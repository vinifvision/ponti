import { bandeiraDoCartao, formatarNumeroCartao, formatarValidade, numeroCartaoValido } from './cartao.utils';

describe('cartao.utils', () => {
  it('formata número e validade enquanto a pessoa digita', () => {
    expect(formatarNumeroCartao('4242424242424242')).toBe('4242 4242 4242 4242');
    expect(formatarValidade('1235')).toBe('12/35');
    expect(formatarValidade('1')).toBe('1');
  });

  it('valida pelo algoritmo de Luhn e reconhece a bandeira', () => {
    expect(numeroCartaoValido('4242 4242 4242 4242')).toBeTrue();
    expect(numeroCartaoValido('4242 4242 4242 4241')).toBeFalse();
    expect(bandeiraDoCartao('4242')).toBe('Visa');
    expect(bandeiraDoCartao('5555 5555')).toBe('Mastercard');
  });
});
