import { Injectable, computed, signal } from '@angular/core';

export type TipoUsuario = 'STARTUP' | 'MENTOR';

// Quem é a pessoa logada (UsuarioResumo no backend)
export interface UsuarioLogado {
  id: number;
  email: string;
  tipo: TipoUsuario;
  startupId: number | null;
  mentorId: number | null;
  nome: string | null;
}

// Resposta do backend em POST /auth/login
export interface DadosSessao {
  token: string;
  tipoToken: string;
  expiraEm: string;
  usuario: UsuarioLogado;
}

const CHAVE = 'ponti.sessao';

// Guarda o token do login e quem está usando o sistema.
// Os dados ficam no localStorage para sobreviver ao F5.
@Injectable({ providedIn: 'root' })
export class SessaoService {
  readonly dados = signal<DadosSessao | null>(this.ler());

  readonly usuario = computed(() => this.dados()?.usuario ?? null);
  readonly token = computed(() => this.dados()?.token ?? null);
  readonly tipo = computed(() => this.usuario()?.tipo ?? null);
  readonly nome = computed(() => this.usuario()?.nome ?? null);
  readonly startupId = computed(() => this.usuario()?.startupId ?? null);
  readonly mentorId = computed(() => this.usuario()?.mentorId ?? null);

  // Logado = tem sessão e o token ainda não venceu
  logado() {
    const dados = this.dados();
    return dados !== null && new Date(dados.expiraEm).getTime() > Date.now();
  }

  entrar(dados: DadosSessao) {
    this.dados.set(dados);
    try {
      localStorage.setItem(CHAVE, JSON.stringify(dados));
    } catch {
      // Sem localStorage (ex.: modo privado): a sessão vale só até recarregar a página.
    }
  }

  sair() {
    this.dados.set(null);
    try {
      localStorage.removeItem(CHAVE);
    } catch {
      // nada a fazer
    }
  }

  private ler(): DadosSessao | null {
    try {
      const texto = localStorage.getItem(CHAVE);
      const dados = texto ? (JSON.parse(texto) as DadosSessao) : null;
      // Sessão salva no formato antigo (sem token) não serve mais
      return dados?.token && dados.usuario ? dados : null;
    } catch {
      return null;
    }
  }
}
