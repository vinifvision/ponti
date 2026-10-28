import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { Certificacao } from '../models';

export type DonoCertificacao = 'startups' | 'mentores';

// Certificações (premiações, programas, cursos) do perfil, para startup e mentor:
//   GET/POST /{startups|mentores}/{id}/certificacoes  e  DELETE .../{certificacaoId}
@Injectable({ providedIn: 'root' })
export class CertificacaoService {
  private http = inject(HttpClient);

  listar(dono: DonoCertificacao, id: number): Observable<Certificacao[]> {
    return this.http.get<Certificacao[]>(`${API_URL}/${dono}/${id}/certificacoes`);
  }

  criar(dono: DonoCertificacao, id: number, certificacao: Certificacao): Observable<Certificacao> {
    return this.http.post<Certificacao>(`${API_URL}/${dono}/${id}/certificacoes`, certificacao);
  }

  remover(dono: DonoCertificacao, id: number, certificacaoId: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/${dono}/${id}/certificacoes/${certificacaoId}`);
  }
}
