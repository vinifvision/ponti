import { PerfilLogadoService } from '../../auth/perfil-logado.service';
import {
  Component,
  ElementRef,
  HostListener,
  WritableSignal,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { AgendaService } from '../../agenda/agenda.service';
import { rotaInicialDe } from '../../auth/acesso';
import { ConviteService } from '../../convites/convite.service';
import { MentorService } from '../../mentores/mentor.service';
import { StartupService } from '../../startups/startup.service';
import {
  OPCOES_EXPERIENCIA,
  OPCOES_MATURIDADE,
  OPCOES_ORDENACAO_MENTORES,
  OPCOES_ORDENACAO_STARTUPS,
  OPCOES_PRECO,
  opcoesDeArea,
  opcoesDeSegmento,
} from '../../shared/busca-opcoes';
import { OpcaoFiltro, FiltroSelectComponent } from '../../shared/filtro-select/filtro-select.component';
import { contarPedidosPendentes, contarSugestoesPendentes, descreverPendencias } from '../../shared/pendencias';
import { SessaoService } from '../../auth/sessao.service';
import { IconeComponent, NomeIcone } from '../../shared/icone/icone.component';
import { BuscaService } from '../busca.service';

interface ItemNavegacao {
  rotulo: string;
  rota: string;
  icone: NomeIcone;
  // Telas "filhas" que também deixam este item em destaque (ex.: perfil do mentor -> Encontrar Mentores)
  relacionadas?: string[];
}

// Um filtro do painel que abre junto com a busca
interface FiltroDaBusca {
  rotulo: string;
  placeholder: string;
  limpavel: boolean;
  opcoes: OpcaoFiltro[];
  aplicado: WritableSignal<string>;
}

// Navbar do topo: links das telas, busca com filtros, sino e menu da conta.
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink, IconeComponent, FiltroSelectComponent],
  // "contents": o cabeçalho fica fixo no topo da página inteira (sticky em relação ao shell)
  host: { class: 'contents' },
  templateUrl: './topbar.component.html',
})
export class TopbarComponent {
  private elemento = inject(ElementRef<HTMLElement>);
  private router = inject(Router);
  private sessao = inject(SessaoService);
  private perfilLogado = inject(PerfilLogadoService);
  private agendaService = inject(AgendaService);
  private conviteService = inject(ConviteService);
  private mentorService = inject(MentorService);
  private startupService = inject(StartupService);
  readonly busca = inject(BuscaService);

  private campoBusca = viewChild<ElementRef<HTMLInputElement>>('campoBusca');

  menuAberto = signal(false);
  // Busca "esticada": esconde os rótulos dos links, escurece a tela e mostra os filtros
  buscaAberta = signal(false);
  // O que está digitado e selecionado no painel; só vale para a lista depois do Enter
  termoDigitado = signal(this.busca.termo());
  rascunho = signal<string[]>([]);
  private carregandoOpcoes = false;
  // Tela aberta agora (sem parâmetros), para destacar o link correspondente
  private urlAtual = signal(this.caminhoDe(this.router.url));

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
      .subscribe((e) => {
        this.urlAtual.set(this.caminhoDe((e as NavigationEnd).urlAfterRedirects));
        this.menuAberto.set(false);
        this.atualizarPendencias();
      });
  }

  readonly inicio = computed(() => rotaInicialDe(this.sessao.tipo()));

  // O mentor busca startups; a startup busca mentores
  textoBusca = computed(() => (this.sessao.tipo() === 'MENTOR' ? 'Buscar startups...' : 'Buscar mentores...'));

  // Startup procura mentores e acompanha as sessões; mentor define a agenda, confirma pedidos e procura startups.
  readonly itens = computed<ItemNavegacao[]>(() =>
    this.sessao.tipo() === 'MENTOR'
      ? [
          { rotulo: 'Encontrar Startups', rota: '/encontrar-startups', icone: 'busca', relacionadas: ['/startups'] },
          { rotulo: 'Mentorias', rota: '/mentorias', icone: 'agenda' },
          { rotulo: 'Minha agenda', rota: '/agenda', icone: 'agenda' },
          { rotulo: 'Meu perfil', rota: '/perfil', icone: 'usuario' },
        ]
      : [
          { rotulo: 'Encontrar Mentores', rota: '/encontrar-mentores', icone: 'busca', relacionadas: ['/mentores'] },
          { rotulo: 'Mentorias', rota: '/mentorias', icone: 'agenda' },
          { rotulo: 'Meu perfil', rota: '/perfil', icone: 'usuario' },
        ],
  );

  // Filtros do painel: os mesmos das telas de busca de cada tipo de conta
  readonly filtros = computed<FiltroDaBusca[]>(() =>
    this.sessao.tipo() === 'MENTOR'
      ? [
          { rotulo: 'Segmento', placeholder: 'Todos', limpavel: true, opcoes: this.busca.opcoesSegmento(), aplicado: this.busca.segmento },
          { rotulo: 'Maturidade', placeholder: 'Todas', limpavel: true, opcoes: OPCOES_MATURIDADE, aplicado: this.busca.maturidade },
          { rotulo: 'Ordenar', placeholder: 'Mais compatíveis', limpavel: false, opcoes: OPCOES_ORDENACAO_STARTUPS, aplicado: this.busca.ordenacaoStartups },
        ]
      : [
          { rotulo: 'Área de atuação', placeholder: 'Todas', limpavel: true, opcoes: this.busca.opcoesArea(), aplicado: this.busca.area },
          { rotulo: 'Experiência', placeholder: 'Todas', limpavel: true, opcoes: OPCOES_EXPERIENCIA, aplicado: this.busca.experiencia },
          { rotulo: 'Faixa de preço', placeholder: 'Qualquer valor', limpavel: true, opcoes: OPCOES_PRECO, aplicado: this.busca.faixaPreco },
          { rotulo: 'Ordenar', placeholder: 'Mais compatíveis', limpavel: false, opcoes: OPCOES_ORDENACAO_MENTORES, aplicado: this.busca.ordenacaoMentores },
        ],
  );

  // Nome e tipo de quem está logado
  usuario = computed(() => ({
    nome: this.perfilLogado.nome() ?? this.sessao.nome() ?? 'Visitante',
    foto: this.perfilLogado.foto(),
    papel: this.sessao.tipo() === 'MENTOR' ? 'Mentor' : 'Startup',
  }));

  // Link da tela aberta: fica roxo (e leva aria-current para leitores de tela)
  ativo(item: ItemNavegacao): boolean {
    const url = this.urlAtual();
    return [item.rota, ...(item.relacionadas ?? [])].some((rota) => url === rota || url.startsWith(rota + '/'));
  }

  // Configurações fica dentro do menu da conta; quando a tela está aberta, o item aparece em destaque lá
  emConfiguracoes(): boolean {
    return this.urlAtual() === '/configuracoes';
  }

  private caminhoDe(url: string): string {
    return url.split(/[?#]/)[0];
  }

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

  // ---- Busca ----

  abrirBusca() {
    if (this.buscaAberta()) return;
    this.menuAberto.set(false);
    // O painel começa mostrando o que já está aplicado na lista
    this.termoDigitado.set(this.busca.termo());
    this.rascunho.set(this.filtros().map((f) => f.aplicado()));
    this.buscaAberta.set(true);
    this.carregarOpcoesSeFaltar();
  }

  fecharBusca() {
    this.buscaAberta.set(false);
  }

  // "X" no lugar da lupa (aparece depois do Enter): cancela a busca e limpa o texto e os filtros
  limparBusca() {
    this.busca.limparBusca();
    this.termoDigitado.set('');
    this.rascunho.set(this.filtros().map((f) => f.aplicado()));
    this.buscaAberta.set(false);
    this.campoBusca()?.nativeElement.blur();
  }

  valorDoRascunho(indice: number): string {
    return this.rascunho()[indice] ?? '';
  }

  definirRascunho(indice: number, valor: string) {
    this.rascunho.update((lista) => lista.map((v, i) => (i === indice ? valor : v)));
  }

  // Enter: aplica o termo e os filtros escolhidos, vai para a tela de busca (se estiver em outra) e fecha o painel.
  aplicarBusca() {
    this.busca.termo.set(this.termoDigitado().trim());
    this.filtros().forEach((f, i) => f.aplicado.set(this.rascunho()[i] ?? ''));

    const destino = this.inicio();
    if (this.urlAtual() !== destino) {
      this.router.navigateByUrl(destino);
    }
    this.buscaAberta.set(false);
    this.campoBusca()?.nativeElement.blur();
  }

  // Fora da tela de busca ainda não há lista carregada: busca só para montar as opções de área/segmento
  private carregarOpcoesSeFaltar() {
    if (this.carregandoOpcoes) return;

    if (this.sessao.tipo() === 'MENTOR') {
      if (this.busca.opcoesSegmento().length > 0) return;
      this.carregandoOpcoes = true;
      this.startupService.recomendadas().subscribe({
        next: (lista) => this.busca.opcoesSegmento.set(opcoesDeSegmento(lista)),
        error: () => {},
        complete: () => (this.carregandoOpcoes = false),
      });
    } else {
      if (this.busca.opcoesArea().length > 0) return;
      this.carregandoOpcoes = true;
      this.mentorService.recomendados().subscribe({
        next: (lista) => this.busca.opcoesArea.set(opcoesDeArea(lista)),
        error: () => {},
        complete: () => (this.carregandoOpcoes = false),
      });
    }
  }

  // ---- Conta ----

  alternarMenu() {
    this.fecharBusca();
    this.menuAberto.update((aberto) => !aberto);
  }

  sair() {
    this.menuAberto.set(false);
    this.buscaAberta.set(false);
    this.busca.limpar();
    this.sessao.sair();
    this.router.navigateByUrl('/bem-vindo');
  }

  @HostListener('document:click', ['$event'])
  fecharAoClicarFora(evento: MouseEvent) {
    const dentro = this.elemento.nativeElement.contains(evento.target as Node);
    if (!dentro) {
      this.menuAberto.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  fecharComEsc() {
    this.menuAberto.set(false);
    this.fecharBusca();
  }

  // Tab para fora da busca (para um link, por exemplo) também recolhe o painel
  fecharSeFocoSaiu(evento: FocusEvent, caixa: HTMLElement) {
    const proximo = evento.relatedTarget as Node | null;
    if (proximo && !caixa.contains(proximo)) {
      this.fecharBusca();
    }
  }
}
