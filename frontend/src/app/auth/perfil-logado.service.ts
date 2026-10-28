import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { MentorService } from '../mentores/mentor.service';
import { StartupService } from '../startups/startup.service';
import { SessaoService } from './sessao.service';

// Foto e nome do perfil de quem está logado, para a barra do topo.
// A tela "Meu perfil" chama atualizar() depois de salvar, e o topo muda na hora.
@Injectable({ providedIn: 'root' })
export class PerfilLogadoService {
  private sessao = inject(SessaoService);
  private mentorService = inject(MentorService);
  private startupService = inject(StartupService);

  readonly foto = signal<string | null>(null);
  readonly nome = signal<string | null>(null);

  carregar() {
    const mentorId = this.sessao.mentorId();
    const startupId = this.sessao.startupId();
    const pedido: Observable<{ nome: string; fotoUrl?: string | null }> | null = mentorId !== null ? this.mentorService.buscar(mentorId) : startupId !== null ? this.startupService.buscar(startupId) : null;
    pedido?.subscribe({
      next: (perfil) => this.atualizar(perfil.nome, perfil.fotoUrl ?? null),
      error: () => {},
    });
  }

  atualizar(nome: string, foto: string | null) {
    this.nome.set(nome);
    this.foto.set(foto);
  }
}
