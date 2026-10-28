import { CamposConta, SENHA_MAXIMA, SENHA_MINIMA, validarConta } from './validacao-cadastro';

const valida: CamposConta = {
  nome: 'PetCare Digital',
  areaAtuacao: 'PetTech',
  sobre: 'Conectamos tutores a veterinários.',
  email: 'contato@petcare.com',
  senha: '12345678',
};

describe('validarConta', () => {
  it('aceita dados corretos', () => {
    expect(validarConta(valida)).toBe('');
  });

  it('exige nome, área de atuação e "sobre" (espaços não contam)', () => {
    expect(validarConta({ ...valida, nome: '   ' })).toContain('nome');
    expect(validarConta({ ...valida, areaAtuacao: '' })).toContain('área de atuação');
    expect(validarConta({ ...valida, sobre: ' ' })).toContain('Sobre');
  });

  it('respeita os tamanhos máximos do backend', () => {
    expect(validarConta({ ...valida, nome: 'a'.repeat(256) })).toContain('255');
    expect(validarConta({ ...valida, areaAtuacao: 'a'.repeat(256) })).toContain('255');
    expect(validarConta({ ...valida, sobre: 'a'.repeat(2001) })).toContain('2000');
  });

  it('rejeita e-mail inválido e aceita com espaços nas pontas', () => {
    expect(validarConta({ ...valida, email: 'sem-arroba' })).toContain('e-mail');
    expect(validarConta({ ...valida, email: 'a@b' })).toContain('e-mail');
    expect(validarConta({ ...valida, email: '  contato@petcare.com ' })).toBe('');
  });

  it('senha: mínimo de 8 e máximo de 72 caracteres', () => {
    expect(validarConta({ ...valida, senha: 'a'.repeat(SENHA_MINIMA - 1) })).toContain(`${SENHA_MINIMA}`);
    expect(validarConta({ ...valida, senha: 'a'.repeat(SENHA_MINIMA) })).toBe('');
    expect(validarConta({ ...valida, senha: 'a'.repeat(SENHA_MAXIMA) })).toBe('');
    expect(validarConta({ ...valida, senha: 'a'.repeat(SENHA_MAXIMA + 1) })).toContain(`${SENHA_MAXIMA}`);
  });
});
