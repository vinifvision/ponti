import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CrudService } from '../crud.service';
import { Certificacao, Startup } from '../models';

@Injectable({ providedIn: 'root' })
export class StartupService extends CrudService<Startup> {
  constructor(http: HttpClient) {
    super(http, 'startups');
  }

  // GET /startups/{id}/certificacoes -> conquistas e programas da startup
  certificacoes(startupId: number): Observable<Certificacao[]> {
    return this.http.get<Certificacao[]>(`${this.url}/${startupId}/certificacoes`);
  }

  // PATCH: muda só os campos enviados (tela "Meu perfil")
  atualizarParcial(id: number, campos: Partial<Startup>): Observable<Startup> {
    return this.http.patch<Startup>(`${this.url}/${id}`, campos);
  }

  // Troca só o nível de maturidade (1 a 9)
  atualizarMaturidade(id: number, nivel: number): Observable<Startup> {
    return this.atualizarParcial(id, { estagioMaturidade: nivel });
  }
}
