import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { CrudService } from '../crud.service';
import { AvaliacaoPublica, Certificacao, Mentor } from '../models';

@Injectable({ providedIn: 'root' })
export class MentorService extends CrudService<Mentor> {
  constructor(http: HttpClient) {
    super(http, 'mentores');
  }

  // GET /mentores/recomendados -> mentores do mais ao menos compatível com a startup logada.
  // O backend devolve { mentor, compatibilidade, motivos }; aqui vira um Mentor com esses dois campos a mais.
  recomendados(): Observable<Mentor[]> {
    return this.http
      .get<{ mentor: Mentor; compatibilidade: number; motivos: string[] }[]>(`${this.url}/recomendados`)
      .pipe(map((lista) => lista.map((r) => ({ ...r.mentor, compatibilidade: r.compatibilidade, motivos: r.motivos }))));
  }

  // GET /mentores/{id}/certificacoes -> seção "Experiência" do perfil
  certificacoes(mentorId: number): Observable<Certificacao[]> {
    return this.http.get<Certificacao[]>(`${this.url}/${mentorId}/certificacoes`);
  }

  // GET /mentores/{id}/avaliacoes -> o que as startups disseram, mais recentes primeiro
  avaliacoes(mentorId: number): Observable<AvaliacaoPublica[]> {
    return this.http.get<AvaliacaoPublica[]>(`${this.url}/${mentorId}/avaliacoes`);
  }

  // PATCH: muda só os campos enviados (tela "Meu perfil")
  atualizarParcial(id: number, campos: Partial<Mentor>): Observable<Mentor> {
    return this.http.patch<Mentor>(`${this.url}/${id}`, campos);
  }
}
