import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../api.config';
import { DadosSessao, SessaoService } from './sessao.service';

export interface CadastroStartup {
  usuario: string;
  senha: string;
  nome: string;
  descricao?: string;
  segmento?: string;
  cidade?: string;
  estado?: string;
  estagioMaturidade: number;
}

export interface CadastroMentor {
  usuario: string;
  senha: string;
  nome: string;
  bio?: string;
  areasAtuacao?: string;
  ecossistemas?: string;
  valorSessao?: number;
}

// Fala com /auth/* do backend e, se deu certo, guarda a sessão.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private sessao = inject(SessaoService);

  login(usuario: string, senha: string): Observable<DadosSessao> {
    return this.http
      .post<DadosSessao>(`${API_URL}/auth/login`, { usuario, senha })
      .pipe(tap((dados) => this.sessao.entrar(dados)));
  }

  cadastrarStartup(dados: CadastroStartup): Observable<DadosSessao> {
    return this.http
      .post<DadosSessao>(`${API_URL}/auth/cadastro/startup`, dados)
      .pipe(tap((resposta) => this.sessao.entrar(resposta)));
  }

  cadastrarMentor(dados: CadastroMentor): Observable<DadosSessao> {
    return this.http
      .post<DadosSessao>(`${API_URL}/auth/cadastro/mentor`, dados)
      .pipe(tap((resposta) => this.sessao.entrar(resposta)));
  }
}
