import { atendeFaixaExperiencia } from './experiencia.utils';

describe('atendeFaixaExperiencia', () => {
  it('sem faixa escolhida, qualquer mentor passa (mesmo sem anos informados)', () => {
    expect(atendeFaixaExperiencia(7, '')).toBeTrue();
    expect(atendeFaixaExperiencia(null, '')).toBeTrue();
    expect(atendeFaixaExperiencia(undefined, '')).toBeTrue();
  });

  it('com faixa escolhida, mentor sem anos informados não passa', () => {
    expect(atendeFaixaExperiencia(null, 'senior')).toBeFalse();
    expect(atendeFaixaExperiencia(undefined, 'iniciante')).toBeFalse();
  });

  it('iniciante: até 2 anos (inclusive o 0 e o 2)', () => {
    expect(atendeFaixaExperiencia(0, 'iniciante')).toBeTrue();
    expect(atendeFaixaExperiencia(2, 'iniciante')).toBeTrue();
    expect(atendeFaixaExperiencia(3, 'iniciante')).toBeFalse();
  });

  it('intermediário: de 3 a 5 anos', () => {
    expect(atendeFaixaExperiencia(2, 'intermediario')).toBeFalse();
    expect(atendeFaixaExperiencia(3, 'intermediario')).toBeTrue();
    expect(atendeFaixaExperiencia(5, 'intermediario')).toBeTrue();
    expect(atendeFaixaExperiencia(6, 'intermediario')).toBeFalse();
  });

  it('sênior: mais de 5 anos', () => {
    expect(atendeFaixaExperiencia(5, 'senior')).toBeFalse();
    expect(atendeFaixaExperiencia(6, 'senior')).toBeTrue();
    expect(atendeFaixaExperiencia(30, 'senior')).toBeTrue();
  });

  it('cada mentor cai em exatamente uma faixa', () => {
    for (let anos = 0; anos <= 70; anos++) {
      const faixas = ['iniciante', 'intermediario', 'senior'].filter((f) => atendeFaixaExperiencia(anos, f));
      expect(faixas.length).withContext(`${anos} anos`).toBe(1);
    }
  });
});
