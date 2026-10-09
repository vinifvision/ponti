import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
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
  readonly busca = inject(BuscaService);

  menuAberto = signal(false);
  temNotificacao = signal(true);

  readonly usuario = { nome: 'Startup', papel: 'Administrador' };

  alternarMenu() {
    this.menuAberto.update((aberto) => !aberto);
  }

  sair() {
    this.menuAberto.set(false);
    // TODO: chamar o serviço de autenticação quando o login existir no backend.
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
