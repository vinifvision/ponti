import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { CrudService } from '../crud.service';
import { Startup } from '../models';

@Injectable({ providedIn: 'root' })
export class StartupService extends CrudService<Startup> {
  constructor(http: HttpClient) {
    super(http, 'startups');
  }

  // O PUT da API substitui a startup inteira, então primeiro busca os dados atuais
  // e devolve tudo igual, trocando só o nível de maturidade (1 a 9).
  atualizarMaturidade(id: number, nivel: number): Observable<Startup> {
    return this.buscar(id).pipe(
      switchMap((startup) => this.atualizar(id, { ...startup, estagioMaturidade: nivel })),
    );
  }
}
