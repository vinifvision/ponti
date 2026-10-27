import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CrudService } from '../crud.service';
import { Certificacao, Mentor } from '../models';

@Injectable({ providedIn: 'root' })
export class MentorService extends CrudService<Mentor> {
  constructor(http: HttpClient) {
    super(http, 'mentores');
  }

  // GET /mentores/{id}/certificacoes -> seção "Experiência" do perfil
  certificacoes(mentorId: number): Observable<Certificacao[]> {
    return this.http.get<Certificacao[]>(`${this.url}/${mentorId}/certificacoes`);
  }
}
