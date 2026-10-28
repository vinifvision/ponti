import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CrudService } from '../crud.service';
import { AvaliacaoPublica, Certificacao, Mentor } from '../models';

@Injectable({ providedIn: 'root' })
export class MentorService extends CrudService<Mentor> {
  constructor(http: HttpClient) {
    super(http, 'mentores');
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
