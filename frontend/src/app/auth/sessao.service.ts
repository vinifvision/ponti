import { Injectable, computed, signal } from '@angular/core';

export type TipoUsuario = 'STARTUP' | 'MENTOR';

// Resposta do backend em /auth/login e /auth/cadastro/*
export interface DadosSessao {
  usuarioId: number;
  usuario: string;
  tipo: TipoUsuario;
  startupId: number | null;
  mentorId: number | null;
  nome: string;
}

const CHAVE = 'ponti.sessao';

// Guarda quem está usando o sistema. Os dados ficam no localStorage para sobreviver ao F5.
// (O backend ainda não usa token: ele devolve quem é a pessoa e o front guarda isso.)
@Injectable({ providedIn: 'root' })
export class SessaoService {
  readonly dados = signal<DadosSessao | null>(this.ler());

  readonly logado = computed(() => this.dados() !== null);
  readonly tipo = computed(() => this.dados()?.tipo ?? null);
  readonly startupId = computed(() => this.dados()?.startupId ?? null);
  readonly mentorId = computed(() => this.dados()?.mentorId ?? null);

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
      return texto ? (JSON.parse(texto) as DadosSessao) : null;
    } catch {
      return null;
    }
  }
}
