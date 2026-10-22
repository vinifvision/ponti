import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { CrudService } from '../crud.service';
import { Convite, ConviteDetalhe } from '../models';

@Injectable({ providedIn: 'root' })
export class ConviteService extends CrudService<Convite> {
  constructor(http: HttpClient) {
    super(http, 'convites');
  }

  // GET /convites?startupId=1 -> convites enviados por essa startup
  listarEnviados(startupId: number): Observable<Convite[]> {
    return this.http.get<Convite[]>(this.url, { params: { startupId } });
  }

  // GET /convites/detalhados?startupId=1 -> enviados, já com os dados do mentor
  detalhadosDaStartup(startupId: number): Observable<ConviteDetalhe[]> {
    return this.http.get<ConviteDetalhe[]>(`${this.url}/detalhados`, { params: { startupId } });
  }

  // GET /convites/detalhados?mentorId=1 -> inbox do mentor, já com os dados da startup
  detalhadosDoMentor(mentorId: number): Observable<ConviteDetalhe[]> {
    return this.http.get<ConviteDetalhe[]>(`${this.url}/detalhados`, { params: { mentorId } });
  }

  aceitar(id: number): Observable<Convite> {
    return this.http.post<Convite>(`${API_URL}/convites/${id}/aceitar`, {});
  }

  recusar(id: number): Observable<Convite> {
    return this.http.post<Convite>(`${API_URL}/convites/${id}/recusar`, {});
  }
}
