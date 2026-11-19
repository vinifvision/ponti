import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { EMPTY, Observable, expand, map, reduce } from 'rxjs';
import { CrudService } from '../crud.service';
import { AvaliacaoPublica, Certificacao, ContatoParcial, ContatoPrivado, Mentor, Pagina } from '../models';

// Maior página que a API aceita em GET /mentores e /mentores/recomendados
const TAMANHO_MAXIMO = 50;

@Injectable({ providedIn: 'root' })
export class MentorService extends CrudService<Mentor> {
  constructor(http: HttpClient) {
    super(http, 'mentores');
  }

  // GET /mentores/recomendados -> mentores na ordem de recomendação para a startup logada.
  // O backend devolve páginas de { mentor, compatibilidade, motivos }; aqui vira um Mentor com esses dois campos a mais.
  // Busca todas as páginas porque a tela "Encontrar mentores" ainda filtra e ordena no navegador
  // (a etapa 6 da busca troca isso pelos filtros e pela paginação do servidor).
  recomendados(): Observable<Mentor[]> {
    return this.todasAsPaginas<{ mentor: Mentor; compatibilidade: number; motivos: string[] }>(`${this.url}/recomendados`)
      .pipe(map((lista) => lista.map((r) => ({ ...r.mentor, compatibilidade: r.compatibilidade, motivos: r.motivos }))));
  }

  // GET /mentores (paginado, em ordem de nome): todas as páginas, como a lista simples de antes
  override listar(): Observable<Mentor[]> {
    return this.todasAsPaginas<Mentor>(this.url);
  }

  // GET /mentores/areas -> todas as áreas atendidas (opções do filtro "Área de atuação")
  areas(): Observable<string[]> {
    return this.http.get<string[]>(`${this.url}/areas`);
  }

  // Pede a página 0 e, enquanto houver próxima, a seguinte; junta os itens de todas numa lista só
  private todasAsPaginas<T>(url: string): Observable<T[]> {
    const pagina = (numero: number) =>
      this.http.get<Pagina<T>>(url, { params: { pagina: numero, tamanho: TAMANHO_MAXIMO } });
    return pagina(0).pipe(
      expand((p) => (p.pagina + 1 < p.totalPaginas ? pagina(p.pagina + 1) : EMPTY)),
      reduce((todos, p) => todos.concat(p.itens), [] as T[]),
    );
  }

  // GET /mentores/{id}/certificacoes -> seção "Experiência" do perfil
  certificacoes(mentorId: number): Observable<Certificacao[]> {
    return this.http.get<Certificacao[]>(`${this.url}/${mentorId}/certificacoes`);
  }

  // GET /mentores/{id}/avaliacoes -> o que as startups disseram, mais recentes primeiro
  avaliacoes(mentorId: number): Observable<AvaliacaoPublica[]> {
    return this.http.get<AvaliacaoPublica[]>(`${this.url}/${mentorId}/avaliacoes`);
  }

  // GET /mentores/{id}/contato -> WhatsApp e autorização (só o próprio dono pode ver)
  contato(id: number): Observable<ContatoPrivado> {
    return this.http.get<ContatoPrivado>(`${this.url}/${id}/contato`);
  }

  // PATCH: muda só os campos enviados (tela "Meu perfil"). O WhatsApp vai junto, mas nunca volta na resposta.
  atualizarParcial(id: number, campos: Partial<Mentor> & ContatoParcial): Observable<Mentor> {
    return this.http.patch<Mentor>(`${this.url}/${id}`, campos);
  }
}
