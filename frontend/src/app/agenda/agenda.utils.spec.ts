import { diaSemanaDe, minutosDoDia, validarFaixas } from './agenda.utils';

describe('diaSemanaDe', () => {
  it('descobre o dia da semana de uma data', () => {
    expect(diaSemanaDe('2026-10-29')).toBe('THURSDAY');
    expect(diaSemanaDe('2026-11-01')).toBe('SUNDAY');
    expect(diaSemanaDe('2026-11-02')).toBe('MONDAY');
  });
});

describe('minutosDoDia', () => {
  it('converte horário em minutos', () => {
    expect(minutosDoDia('00:00')).toBe(0);
    expect(minutosDoDia('09:30')).toBe(570);
    expect(minutosDoDia('14:00:00')).toBe(840);
  });

  it('texto vazio ou inválido vira NaN', () => {
    expect(minutosDoDia('')).toBeNaN();
    expect(minutosDoDia('abc')).toBeNaN();
  });
});

describe('validarFaixas', () => {
  const faixa = (horaInicio: string, horaFim: string) => ({ horaInicio, horaFim });

  it('aceita faixas válidas, inclusive encostadas', () => {
    expect(validarFaixas([faixa('09:00', '12:00'), faixa('14:00', '16:00')])).toBe('');
    expect(validarFaixas([faixa('12:00', '14:00'), faixa('14:00', '16:00')])).toBe('');
  });

  it('lista vazia é válida', () => {
    expect(validarFaixas([])).toBe('');
  });

  it('recusa horário vazio', () => {
    expect(validarFaixas([faixa('', '12:00')])).toContain('Preencha');
  });

  it('recusa término antes ou igual ao início', () => {
    expect(validarFaixas([faixa('12:00', '09:00')])).toContain('depois do início');
    expect(validarFaixas([faixa('09:00', '09:00')])).toContain('depois do início');
  });

  it('recusa minutos fora do passo (13:13)', () => {
    expect(validarFaixas([faixa('13:13', '23:00')])).toContain('meia hora');
  });

  it('recusa faixa menor que a sessão', () => {
    expect(validarFaixas([faixa('09:00', '09:30')], 60)).toContain('60 minutos');
    expect(validarFaixas([faixa('09:00', '09:30')], 30)).toBe('');
  });

  it('recusa faixas sobrepostas, em qualquer ordem', () => {
    expect(validarFaixas([faixa('09:00', '12:00'), faixa('11:00', '13:00')])).toContain('sobrepostos');
    expect(validarFaixas([faixa('14:00', '16:00'), faixa('09:00', '15:00')])).toContain('sobrepostos');
  });
});
