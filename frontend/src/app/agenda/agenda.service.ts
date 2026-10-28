import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import { Agendamento, FaixaDisponibilidade, NovoAgendamento } from '../models';

// Fala com a agenda do backend: disponibilidade do mentor, horários livres e agendamentos.
@Injectable({ providedIn: 'root' })
export class AgendaService {
  private http = inject(HttpClient);

  disponibilidade(mentorId: number): Observable<FaixaDisponibilidade[]> {
    return this.http.get<FaixaDisponibilidade[]>(`${API_URL}/mentores/${mentorId}/disponibilidade`);
  }

  // Substitui a disponibilidade semanal inteira
  salvarDisponibilidade(mentorId: number, faixas: FaixaDisponibilidade[]): Observable<FaixaDisponibilidade[]> {
    return this.http.put<FaixaDisponibilidade[]>(`${API_URL}/mentores/${mentorId}/disponibilidade`, faixas);
  }

  // Horários livres entre duas datas ("2026-11-01"), como "2026-11-03T09:00:00"
  horariosLivres(mentorId: number, de: string, ate: string): Observable<string[]> {
    return this.http.get<string[]>(`${API_URL}/mentores/${mentorId}/horarios`, { params: { de, ate } });
  }

  daStartup(startupId: number): Observable<Agendamento[]> {
    return this.http.get<Agendamento[]>(`${API_URL}/agendamentos`, { params: { startupId } });
  }

  doMentor(mentorId: number): Observable<Agendamento[]> {
    return this.http.get<Agendamento[]>(`${API_URL}/agendamentos`, { params: { mentorId } });
  }

  buscar(id: number): Observable<Agendamento> {
    return this.http.get<Agendamento>(`${API_URL}/agendamentos/${id}`);
  }

  avaliar(id: number, nota: number, comentario: string): Observable<Agendamento> {
    return this.http.post<Agendamento>(`${API_URL}/agendamentos/${id}/avaliacao`, { nota, comentario });
  }

  agendar(pedido: NovoAgendamento): Observable<Agendamento> {
    return this.http.post<Agendamento>(`${API_URL}/agendamentos`, pedido);
  }

  confirmar(id: number, linkReuniao: string): Observable<Agendamento> {
    return this.http.post<Agendamento>(`${API_URL}/agendamentos/${id}/confirmar`, { linkReuniao });
  }

  recusar(id: number): Observable<Agendamento> {
    return this.http.post<Agendamento>(`${API_URL}/agendamentos/${id}/recusar`, {});
  }

  cancelar(id: number): Observable<Agendamento> {
    return this.http.post<Agendamento>(`${API_URL}/agendamentos/${id}/cancelar`, {});
  }
}
