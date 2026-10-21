import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CrudService } from '../crud.service';
import { Convite } from '../models';

@Injectable({ providedIn: 'root' })
export class ConviteService extends CrudService<Convite> {
  constructor(http: HttpClient) {
    super(http, 'convites');
  }

  // GET /convites?startupId=1 -> convites enviados por essa startup
  listarEnviados(startupId: number): Observable<Convite[]> {
    return this.http.get<Convite[]>(this.url, { params: { startupId } });
  }
}
