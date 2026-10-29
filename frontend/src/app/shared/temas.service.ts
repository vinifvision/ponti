import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { API_URL } from '../api.config';

export interface TemaOpcao {
  codigo: string;
  nome: string;
}

// Lista de temas (Validação de ideia, Pitch e captação...) vinda do backend em GET /temas.
// É a mesma lista para startups (o que precisam) e mentores (em que ajudam), por isso fica num lugar só.
@Injectable({ providedIn: 'root' })
export class TemasService {
  private http = inject(HttpClient);
  private iniciado = false;

  readonly lista = signal<TemaOpcao[]>([]);
  readonly falhou = signal(false);

  // Busca a lista uma única vez; se falhar, a próxima chamada tenta de novo.
  carregar() {
    if (this.iniciado) return;
    this.iniciado = true;
    this.falhou.set(false);
    this.http.get<TemaOpcao[]>(`${API_URL}/temas`).subscribe({
      next: (lista) => this.lista.set(lista),
      error: () => {
        this.iniciado = false;
        this.falhou.set(true);
      },
    });
  }

  nome(codigo: string): string {
    return this.lista().find((t) => t.codigo === codigo)?.nome ?? codigo;
  }
}
