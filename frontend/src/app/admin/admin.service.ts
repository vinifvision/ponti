import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';
import type { TipoUsuario } from '../auth/sessao.service';
import { Dashboard, Pagina, RegistroAuditoria, UsuarioAdmin } from './admin.models';

export interface FiltroUsuarios {
  busca?: string;
  tipo?: TipoUsuario | null;
  situacao?: 'ATIVO' | 'BLOQUEADO' | null;
  pagina?: number;
  tamanho?: number;
}

// Rotas /admin da API (só a conta ADMIN consegue usar; as outras recebem 403)
@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private url = `${API_URL}/admin`;

  // inicio/fim no formato "2026-11-27"; sem eles, a API usa os últimos 30 dias
  dashboard(inicio?: string, fim?: string): Observable<Dashboard> {
    const params: Record<string, string> = {};
    if (inicio) params['inicio'] = inicio;
    if (fim) params['fim'] = fim;
    return this.http.get<Dashboard>(`${this.url}/dashboard`, { params });
  }

  usuarios(filtro: FiltroUsuarios): Observable<Pagina<UsuarioAdmin>> {
    const params: Record<string, string | number> = {};
    if (filtro.busca?.trim()) params['busca'] = filtro.busca.trim();
    if (filtro.tipo) params['tipo'] = filtro.tipo;
    if (filtro.situacao) params['situacao'] = filtro.situacao;
    params['pagina'] = filtro.pagina ?? 0;
    params['tamanho'] = filtro.tamanho ?? 20;
    return this.http.get<Pagina<UsuarioAdmin>>(`${this.url}/usuarios`, { params });
  }

  bloquear(id: number, motivo: string): Observable<UsuarioAdmin> {
    return this.http.post<UsuarioAdmin>(`${this.url}/usuarios/${id}/bloquear`, { motivo });
  }

  desbloquear(id: number, motivo?: string): Observable<UsuarioAdmin> {
    return this.http.post<UsuarioAdmin>(`${this.url}/usuarios/${id}/desbloquear`, { motivo: motivo?.trim() || null });
  }

  auditoria(pagina = 0, tamanho = 20): Observable<Pagina<RegistroAuditoria>> {
    return this.http.get<Pagina<RegistroAuditoria>>(`${this.url}/auditoria`, { params: { pagina, tamanho } });
  }
}
