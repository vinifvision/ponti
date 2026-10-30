import { BuscaService } from './busca.service';

describe('BuscaService', () => {
  let busca: BuscaService;

  beforeEach(() => {
    busca = new BuscaService();
  });

  it('começa sem busca valendo', () => {
    expect(busca.ativa()).toBeFalse();
  });

  it('fica ativa com texto ou com algum filtro, mas não só com ordenação', () => {
    busca.termo.set('  ');
    expect(busca.ativa()).toBeFalse();

    busca.ordenacaoMentores.set('nome');
    expect(busca.ativa()).toBeFalse();

    busca.termo.set('carlos');
    expect(busca.ativa()).toBeTrue();

    busca.termo.set('');
    busca.area.set('Fintechs');
    expect(busca.ativa()).toBeTrue();
  });

  it('limparBusca tira texto e filtros e mantém a ordenação', () => {
    busca.termo.set('carlos');
    busca.area.set('Fintechs');
    busca.maturidade.set('ideacao');
    busca.ordenacaoMentores.set('nome');

    busca.limparBusca();

    expect(busca.ativa()).toBeFalse();
    expect(busca.termo()).toBe('');
    expect(busca.area()).toBe('');
    expect(busca.maturidade()).toBe('');
    expect(busca.ordenacaoMentores()).toBe('nome');
  });

  it('limpar (ao sair da conta) zera também a ordenação', () => {
    busca.ordenacaoStartups.set('nome');
    busca.limpar();
    expect(busca.ordenacaoStartups()).toBe('');
  });
});
