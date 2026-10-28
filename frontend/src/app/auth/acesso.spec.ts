import { decidirAcesso, rotaInicialDe } from './acesso';

describe('rotaInicialDe', () => {
  it('mentor começa procurando startups', () => {
    expect(rotaInicialDe('MENTOR')).toBe('/encontrar-startups');
  });

  it('startup começa procurando mentores', () => {
    expect(rotaInicialDe('STARTUP')).toBe('/encontrar-mentores');
  });

  it('sem tipo definido, usa a tela da startup', () => {
    expect(rotaInicialDe(null)).toBe('/encontrar-mentores');
  });
});

describe('decidirAcesso', () => {
  it('sem login, manda para o login (qualquer que seja a regra da tela)', () => {
    expect(decidirAcesso(false, null)).toBe('ir-para-login');
    expect(decidirAcesso(false, 'MENTOR', ['MENTOR'])).toBe('ir-para-login');
  });

  it('tela aberta a quem está logado: os dois tipos entram', () => {
    expect(decidirAcesso(true, 'STARTUP')).toBe('permitir');
    expect(decidirAcesso(true, 'MENTOR', [])).toBe('permitir');
  });

  it('tela só de mentor: mentor entra, startup volta para o início dela', () => {
    expect(decidirAcesso(true, 'MENTOR', ['MENTOR'])).toBe('permitir');
    expect(decidirAcesso(true, 'STARTUP', ['MENTOR'])).toBe('ir-para-inicio');
  });

  it('tela só de startup: startup entra, mentor volta para o início dele', () => {
    expect(decidirAcesso(true, 'STARTUP', ['STARTUP'])).toBe('permitir');
    expect(decidirAcesso(true, 'MENTOR', ['STARTUP'])).toBe('ir-para-inicio');
  });

  it('logado sem tipo conhecido não passa de uma tela restrita', () => {
    expect(decidirAcesso(true, null, ['MENTOR'])).toBe('ir-para-inicio');
  });
});
