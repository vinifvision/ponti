import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../api.config';
import { Mentor } from '../models';
import { MentorService } from './mentor.service';

// A API devolve os mentores em páginas. Enquanto a tela "Encontrar mentores" filtra no navegador,
// o serviço busca todas as páginas e entrega uma lista só, como antes da paginação.
describe('MentorService', () => {
  let service: MentorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MentorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const item = (id: number) => ({ mentor: { id, nome: `M${id}` }, compatibilidade: 80, motivos: ['Motivo'] });

  it('recomendados junta todas as páginas e mantém a compatibilidade e os motivos', () => {
    let resultado: Mentor[] = [];
    service.recomendados().subscribe((lista) => (resultado = lista));

    const p0 = http.expectOne((r) => r.url === `${API_URL}/mentores/recomendados` && r.params.get('pagina') === '0');
    expect(p0.request.params.get('tamanho')).toBe('50');
    p0.flush({ itens: [item(1), item(2)], pagina: 0, tamanho: 50, totalItens: 3, totalPaginas: 2 });

    const p1 = http.expectOne((r) => r.url === `${API_URL}/mentores/recomendados` && r.params.get('pagina') === '1');
    p1.flush({ itens: [item(3)], pagina: 1, tamanho: 50, totalItens: 3, totalPaginas: 2 });

    expect(resultado.map((m) => m.id)).toEqual([1, 2, 3]);
    expect(resultado[0].compatibilidade).toBe(80);
    expect(resultado[0].motivos).toEqual(['Motivo']);
  });

  it('recomendados com uma página só faz uma requisição', () => {
    let resultado: Mentor[] = [];
    service.recomendados().subscribe((lista) => (resultado = lista));

    http.expectOne((r) => r.url === `${API_URL}/mentores/recomendados`)
      .flush({ itens: [item(1)], pagina: 0, tamanho: 50, totalItens: 1, totalPaginas: 1 });

    expect(resultado.length).toBe(1);
  });

  it('areas lê a lista de áreas do servidor', () => {
    let areas: string[] = [];
    service.areas().subscribe((a) => (areas = a));

    http.expectOne(`${API_URL}/mentores/areas`).flush(['Fintechs', 'SaaS']);

    expect(areas).toEqual(['Fintechs', 'SaaS']);
  });
});
