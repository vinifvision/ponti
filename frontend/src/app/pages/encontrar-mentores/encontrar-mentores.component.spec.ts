import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from '../../api.config';
import { BuscaService } from '../../layout/busca.service';
import { EncontrarMentoresComponent, TAMANHO_PAGINA } from './encontrar-mentores.component';

// A tela só monta a consulta (filtros + página) e mostra a página que o servidor devolve.
// O template é trocado por um vazio: aqui interessa a lógica, não os cards.
describe('EncontrarMentoresComponent', () => {
  let http: HttpTestingController;
  let busca: BuscaService;

  const URL_RECOMENDADOS = `${API_URL}/mentores/recomendados`;

  function pagina(numero: number, totalPaginas: number, ids: number[]) {
    return {
      itens: ids.map((id) => ({ mentor: { id, nome: `M${id}` }, compatibilidade: 50, motivos: [] })),
      pagina: numero,
      tamanho: TAMANHO_PAGINA,
      totalItens: totalPaginas * TAMANHO_PAGINA,
      totalPaginas,
    };
  }

  function criar() {
    const fixture = TestBed.createComponent(EncontrarMentoresComponent);
    fixture.detectChanges();
    TestBed.flushEffects();
    http.expectOne(`${API_URL}/mentores/areas`).flush(['Fintechs', 'SaaS']);
    return fixture.componentInstance;
  }

  // A próxima consulta da busca (roda os effects antes, para a requisição sair)
  function proximaConsulta(): TestRequest {
    TestBed.flushEffects();
    return http.expectOne((r) => r.url === URL_RECOMENDADOS);
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EncontrarMentoresComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).overrideComponent(EncontrarMentoresComponent, { set: { template: '' } });
    http = TestBed.inject(HttpTestingController);
    busca = TestBed.inject(BuscaService);
    busca.limpar();
  });

  afterEach(() => http.verify());

  it('pede a primeira página e mostra o que o servidor devolveu', () => {
    const tela = criar();
    const req = proximaConsulta();
    expect(req.request.params.get('pagina')).toBe('0');
    expect(req.request.params.get('tamanho')).toBe(String(TAMANHO_PAGINA));
    expect(tela.carregando()).toBeTrue();

    req.flush(pagina(0, 3, [1, 2]));

    expect(tela.carregando()).toBeFalse();
    expect(tela.mentores().map((m) => m.id)).toEqual([1, 2]);
    expect(tela.totalPaginas()).toBe(3);
    expect(tela.temAnterior()).toBeFalse();
    expect(tela.temProxima()).toBeTrue();
    expect(busca.opcoesArea().map((o) => o.valor)).toEqual(['Fintechs', 'SaaS']);
  });

  it('a próxima página vem do servidor e a lista anterior fica na tela enquanto carrega', () => {
    const tela = criar();
    proximaConsulta().flush(pagina(0, 3, [1, 2]));

    tela.irParaPagina(1);
    const req = proximaConsulta();
    expect(req.request.params.get('pagina')).toBe('1');
    expect(tela.carregando()).toBeTrue();
    expect(tela.mentores().map((m) => m.id)).toEqual([1, 2]);

    req.flush(pagina(1, 3, [3, 4]));
    expect(tela.pagina()).toBe(1);
    expect(tela.mentores().map((m) => m.id)).toEqual([3, 4]);
  });

  it('mudar um filtro volta para a primeira página e manda o filtro', () => {
    const tela = criar();
    proximaConsulta().flush(pagina(0, 3, [1, 2]));
    tela.irParaPagina(2);
    proximaConsulta().flush(pagina(2, 3, [5]));

    busca.area.set('Fintechs');
    const req = proximaConsulta();

    expect(tela.pagina()).toBe(0);
    expect(req.request.params.get('pagina')).toBe('0');
    expect(req.request.params.get('area')).toBe('Fintechs');
    req.flush(pagina(0, 1, [1]));
  });

  it('uma busca nova cancela a anterior, para uma resposta atrasada não aparecer', () => {
    criar();
    const primeira = proximaConsulta();

    busca.experiencia.set('senior');
    const segunda = proximaConsulta();

    expect(primeira.cancelled).toBeTrue();
    expect(segunda.request.params.get('experiencia')).toBe('senior');
    segunda.flush(pagina(0, 1, [9]));
  });

  it('se a busca falha, mostra o erro', () => {
    const tela = criar();
    proximaConsulta().flush('falhou', { status: 500, statusText: 'Erro' });

    expect(tela.carregando()).toBeFalse();
    expect(tela.erro()).toContain('Não foi possível carregar os mentores');
  });
});
