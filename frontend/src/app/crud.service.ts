import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.config';

// Serviço genérico: os 5 métodos do CRUD falam com /{recurso} da API.
export class CrudService<T extends { id?: number }> {
  protected url: string;

  constructor(protected http: HttpClient, recurso: string) {
    this.url = `${API_URL}/${recurso}`;
  }

  listar(): Observable<T[]> {
    return this.http.get<T[]>(this.url);
  }

  buscar(id: number): Observable<T> {
    return this.http.get<T>(`${this.url}/${id}`);
  }

  criar(item: T): Observable<T> {
    return this.http.post<T>(this.url, item);
  }

  atualizar(id: number, item: T): Observable<T> {
    return this.http.put<T>(`${this.url}/${id}`, item);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
