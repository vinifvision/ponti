import { PerfilLogadoService } from '../../auth/perfil-logado.service';
import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { AgendaService } from '../../agenda/agenda.service';
import { ConviteService } from '../../convites/convite.service';
import { contarPedidosPendentes, contarSugestoesPendentes, descreverPendencias } from '../../shared/pendencias';
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
  private perfilLogado = inject(PerfilLogadoService);
  private agendaService = inject(AgendaService);
  private conviteService = inject(ConviteService);
  readonly busca = inject(BuscaService);

  menuAberto = signal(false);
  // Quantas coisas esperam uma ação da pessoa (veja atualizarPendencias)
  pendencias = signal(0);
  temNotificacao = computed(() => this.pendencias() > 0);
  rotuloNotificacao = computed(() => descreverPendencias(this.pendencias()));

  constructor() {
    this.perfilLogado.carregar();
    // A cada tela aberta, confere de novo: assim o sino acompanha o que a pessoa acabou de fazer.
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.atualizarPendencias());
  }

  // O mentor busca startups; a startup busca mentores
  textoBusca = computed(() => (this.sessao.tipo() === 'MENTOR' ? 'Buscar startups...' : 'Buscar mentores...'));

  // Nome e tipo de quem está logado
  usuario = computed(() => ({
    nome: this.perfilLogado.nome() ?? this.sessao.nome() ?? 'Visitante',
    foto: this.perfilLogado.foto(),
    papel: this.sessao.tipo() === 'MENTOR' ? 'Mentor' : 'Startup',
  }));

  // Mentor: pedidos de mentoria para confirmar. Startup: mentorias sugeridas por mentores.
  // Se a API falhar, o sino fica como estava.
  private atualizarPendencias() {
    const mentorId = this.sessao.mentorId();
    const startupId = this.sessao.startupId();

    if (mentorId !== null) {
      this.agendaService.doMentor(mentorId).subscribe({
        next: (lista) => this.pendencias.set(contarPedidosPendentes(lista)),
        error: () => {},
      });
    } else if (startupId !== null) {
      this.conviteService.listarEnviados(startupId).subscribe({
        next: (lista) => this.pendencias.set(contarSugestoesPendentes(lista)),
        error: () => {},
      });
    }
  }

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
