import { Component, computed, inject } from '@angular/core';
import { SessaoService } from '../../auth/sessao.service';
import { PerfilMentorComponent } from '../perfil-mentor/perfil-mentor.component';
import { PerfilStartupComponent } from '../perfil-startup/perfil-startup.component';

// Rota /perfil: quem está logado vê o PRÓPRIO perfil, do jeito que o outro lado o enxerga.
// O botão "Editar perfil" (dentro de cada perfil) leva para /perfil/editar.
@Component({
  selector: 'app-perfil-proprio',
  standalone: true,
  imports: [PerfilStartupComponent, PerfilMentorComponent],
  template: `
    @if (startupId(); as id) {
      <app-perfil-startup [id]="id" />
    }
    @if (mentorId(); as id) {
      <app-perfil-mentor [id]="id" />
    }
  `,
})
export class PerfilProprioComponent {
  private sessao = inject(SessaoService);

  // Os perfis recebem o id como texto, igual ao parâmetro de rota
  startupId = computed(() => this.texto(this.sessao.startupId()));
  mentorId = computed(() => this.texto(this.sessao.mentorId()));

  private texto(id: number | null): string | null {
    return id === null ? null : String(id);
  }
}
