import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SessaoService } from '../../auth/sessao.service';
import { IconeComponent } from '../../shared/icone/icone.component';
import { BuscaService } from '../busca.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, IconeComponent],
  templateUrl: './topbar.component.html',
})
export class TopbarComponent {
  private elemento = inject(ElementRef<HTMLElement>);
  private router = inject(Router);
  private sessao = inject(SessaoService);
  readonly busca = inject(BuscaService);

  menuAberto = signal(false);
  temNotificacao = signal(true);

  // Nome e tipo de quem está logado
  usuario = computed(() => {
    const dados = this.sessao.dados();
    return {
      nome: dados?.nome ?? 'Visitante',
      papel: dados?.tipo === 'MENTOR' ? 'Mentor' : 'Startup',
    };
  });

  alternarMenu() {
    this.menuAberto.update((aberto) => !aberto);
  }

  sair() {
    this.menuAberto.set(false);
    this.sessao.sair();
    this.router.navigateByUrl('/bem-vindo');
  }

  @HostListener('document:click', ['$event'])
  fecharAoClicarFora(evento: MouseEvent) {
    if (!this.elemento.nativeElement.contains(evento.target as Node)) {
      this.menuAberto.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  fecharComEsc() {
    this.menuAberto.set(false);
  }
}
