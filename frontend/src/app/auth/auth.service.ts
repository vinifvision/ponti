import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, switchMap, tap } from 'rxjs';
import { API_URL } from '../api.config';
import { Certificacao } from '../models';
import { DadosSessao, SessaoService } from './sessao.service';

// Campos do cadastro (iguais aos do backend). O nível de maturidade não entra aqui:
// a startup responde o questionário depois de criar a conta.
export interface CadastroStartup {
  nome: string;
  areaAtuacao: string;
  sobre: string;
  email: string;
  senha: string;
  // Temas em que a startup precisa de ajuda (1 a 3)
  necessidades?: string[];
}

export interface CadastroMentor {
  // Etapa 1: conta
  nome: string;
  email: string;
  senha: string;
  // Etapa 2: perfil profissional
  areaAtuacao: string;
  ecossistemas?: string;
  // Temas em que o mentor ajuda (1 a 5)
  temas?: string[];
  anosExperiencia: number;
  sobre: string;
  fotoUrl?: string;
  videoUrl?: string;
  linkedinUrl?: string;
  certificacoes?: Certificacao[];
  // Etapa 3: como é a mentoria (0 = gratuita)
  valorSessao: number;
  duracaoSessaoMinutos: number;
}

// Fala com /auth/* do backend e, se deu certo, guarda a sessão.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private sessao = inject(SessaoService);

  login(email: string, senha: string): Observable<DadosSessao> {
    return this.http
      .post<DadosSessao>(`${API_URL}/auth/login`, { email, senha })
      .pipe(tap((dados) => this.sessao.entrar(dados)));
  }

  // O cadastro não devolve token, então logo em seguida faz o login com o mesmo e-mail e senha.
  cadastrarStartup(dados: CadastroStartup): Observable<DadosSessao> {
    return this.http
      .post(`${API_URL}/auth/cadastro/startup`, dados)
      .pipe(switchMap(() => this.login(dados.email, dados.senha)));
  }

  cadastrarMentor(dados: CadastroMentor): Observable<DadosSessao> {
    return this.http
      .post(`${API_URL}/auth/cadastro/mentor`, dados)
      .pipe(switchMap(() => this.login(dados.email, dados.senha)));
  }
}
