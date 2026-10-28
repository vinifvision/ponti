import { DadosSessao, SessaoService } from './sessao.service';

function sessaoDe(expiraEm: Date, tipo: 'STARTUP' | 'MENTOR' = 'STARTUP'): DadosSessao {
  return {
    token: 'token-de-teste',
    tipoToken: 'Bearer',
    expiraEm: expiraEm.toISOString(),
    usuario: {
      id: 1,
      email: 'a@b.com',
      tipo,
      startupId: tipo === 'STARTUP' ? 10 : null,
      mentorId: tipo === 'MENTOR' ? 20 : null,
      nome: 'Fulano',
    },
  };
}

const daquiAUmaHora = () => new Date(Date.now() + 60 * 60 * 1000);
const haUmaHora = () => new Date(Date.now() - 60 * 60 * 1000);

describe('SessaoService', () => {
  // O localStorage do navegador sobrevive entre testes: começa sempre limpo.
  beforeEach(() => {
    if (typeof localStorage !== 'undefined') localStorage.clear();
  });

  it('começa sem ninguém logado', () => {
    const sessao = new SessaoService();
    expect(sessao.logado()).toBeFalse();
    expect(sessao.token()).toBeNull();
    expect(sessao.tipo()).toBeNull();
  });

  it('depois de entrar, expõe usuário, tipo e ids do perfil', () => {
    const sessao = new SessaoService();
    sessao.entrar(sessaoDe(daquiAUmaHora(), 'MENTOR'));

    expect(sessao.logado()).toBeTrue();
    expect(sessao.tipo()).toBe('MENTOR');
    expect(sessao.mentorId()).toBe(20);
    expect(sessao.startupId()).toBeNull();
    expect(sessao.nome()).toBe('Fulano');
    expect(sessao.token()).toBe('token-de-teste');
  });

  it('com o token vencido, não conta como logado', () => {
    const sessao = new SessaoService();
    sessao.entrar(sessaoDe(haUmaHora()));
    expect(sessao.logado()).toBeFalse();
  });

  it('sair apaga a sessão', () => {
    const sessao = new SessaoService();
    sessao.entrar(sessaoDe(daquiAUmaHora()));
    sessao.sair();

    expect(sessao.logado()).toBeFalse();
    expect(sessao.usuario()).toBeNull();
  });
});
