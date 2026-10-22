import { Component, ElementRef, HostListener, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SessaoService } from '../../auth/sessao.service';
import { StartupService } from '../../startups/startup.service';
import { IconeComponent } from '../../shared/icone/icone.component';
import { BuscaService } from '../busca.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, IconeComponent],
  templateUrl: './topbar.component.html',
})
export class TopbarComponent implements OnInit {
  private elemento = inject(ElementRef<HTMLElement>);
  private router = inject(Router);
  private sessao = inject(SessaoService);
  private startupService = inject(StartupService);
  readonly busca = inject(BuscaService);

  menuAberto = signal(false);
  temNotificacao = signal(true);

  usuario = signal({ nome: 'Startup', papel: 'Startup' });

  ngOnInit() {
    this.startupService.buscar(this.sessao.startupId()).subscribe({
      next: (startup) => this.usuario.update((u) => ({ ...u, nome: startup.nome })),
      // Se a API falhar, o rótulo genérico "Startup" continua aparecendo.
      error: () => {},
    });
  }

  alternarMenu() {
    this.menuAberto.update((aberto) => !aberto);
  }

  sair() {
    this.menuAberto.set(false);
    // TODO: limpar o token quando o login existir no backend.
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
