import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { ConviteService } from '../../convites/convite.service';
import { BuscaService } from '../../layout/busca.service';
import { Convite, Startup } from '../../models';
import { FiltroSelectComponent, OpcaoFiltro } from '../../shared/filtro-select/filtro-select.component';
import { StartupCardComponent } from '../../startups/startup-card/startup-card.component';
import { StartupService } from '../../startups/startup.service';

// Faixas de maturidade (níveis do questionário TRL) usadas no filtro
const FAIXAS: Record<string, [number, number]> = {
  ideacao: [1, 3],
  validacao: [4, 6],
  operacao: [7, 9],
};

// Tela do mentor: ver as startups da plataforma e sugerir mentoria às que quiser acompanhar.
// A sugestão chega para a startup em Mentorias, com o atalho para o calendário do mentor.
@Component({
  selector: 'app-encontrar-startups',
  standalone: true,
  imports: [FiltroSelectComponent, StartupCardComponent],
  templateUrl: './encontrar-startups.component.html',
})
export class EncontrarStartupsComponent implements OnInit {
  private startupService = inject(StartupService);
  private conviteService = inject(ConviteService);
  private sessao = inject(SessaoService);
  private busca = inject(BuscaService);

  startups = signal<Startup[]>([]);
  convites = signal<Convite[]>([]);
  carregando = signal(true);
  erro = signal('');
  erroConvite = signal('');
  enviandoId = signal<number | null>(null);

  segmento = signal('');
  maturidade = signal('');
  ordenacao = signal('');

  readonly opcoesMaturidade: OpcaoFiltro[] = [
    { valor: 'ideacao', rotulo: 'Ideação (níveis 1 a 3)' },
    { valor: 'validacao', rotulo: 'Validação (níveis 4 a 6)' },
    { valor: 'operacao', rotulo: 'Operação (níveis 7 a 9)' },
  ];

  readonly opcoesOrdenacao: OpcaoFiltro[] = [
    { valor: 'nome', rotulo: 'Nome (A-Z)' },
    { valor: 'mais-madura', rotulo: 'Mais madura primeiro' },
    { valor: 'menos-madura', rotulo: 'Menos madura primeiro' },
  ];

  opcoesSegmento = computed<OpcaoFiltro[]>(() => {
    const segmentos = new Set(this.startups().map((s) => s.segmento?.trim()).filter((s): s is string => !!s));
    return [...segmentos].sort().map((s) => ({ valor: s, rotulo: s }));
  });

  startupsFiltradas = computed(() => {
    const termo = this.busca.termo().trim().toLowerCase();
    const segmento = this.segmento().toLowerCase();
    const faixa = FAIXAS[this.maturidade()];

    const filtradas = this.startups().filter((s) => {
      const texto = [s.nome, s.descricao, s.segmento, s.cidade, s.estado, s.atuacoes].join(' ').toLowerCase();
      const nivel = s.estagioMaturidade ?? 0;
      return (
        (!termo || texto.includes(termo)) &&
        (!segmento || (s.segmento ?? '').trim().toLowerCase() === segmento) &&
        (!faixa || (nivel >= faixa[0] && nivel <= faixa[1]))
      );
    });

    switch (this.ordenacao()) {
      case 'nome':
        return [...filtradas].sort((a, b) => a.nome.localeCompare(b.nome));
      case 'mais-madura':
        return [...filtradas].sort((a, b) => (b.estagioMaturidade ?? 0) - (a.estagioMaturidade ?? 0));
      case 'menos-madura':
        return [...filtradas].sort((a, b) => (a.estagioMaturidade ?? 0) - (b.estagioMaturidade ?? 0));
      default:
        return filtradas;
    }
  });

  ngOnInit() {
    this.startupService.listar().subscribe({
      next: (dados) => {
        this.startups.set(dados);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar as startups. Verifique se a API está rodando.'));
        this.carregando.set(false);
      },
    });

    const mentorId = this.sessao.mentorId();
    if (mentorId !== null) {
      this.conviteService.listarDoMentor(mentorId).subscribe({
        next: (lista) => this.convites.set(lista),
        error: () => {}, // sem a lista, o backend ainda recusa convite repetido (409)
      });
    }
  }

  // Texto do botão quando o mentor já sugeriu mentoria a essa startup
  situacao(startup: Startup): string | null {
    const sugestao = this.convites().find((c) => c.startupId === startup.id && c.enviadoPor === 'MENTOR');
    if (!sugestao) return null;
    if (sugestao.status === 'ACEITO') return 'Ela agendou com você';
    if (sugestao.status === 'RECUSADO') return 'Sugestão dispensada';
    return 'Sugestão enviada';
  }

  convidar(startup: Startup) {
    const mentorId = this.sessao.mentorId();
    const startupId = startup.id;
    if (mentorId === null || startupId == null || this.enviandoId() !== null) return;

    this.erroConvite.set('');
    this.enviandoId.set(startupId);
    this.conviteService.criar({ startupId, mentorId, enviadoPor: 'MENTOR' }).subscribe({
      next: (convite) => {
        this.convites.update((lista) => [...lista, convite]);
        this.enviandoId.set(null);
      },
      error: (e: HttpErrorResponse) => {
        this.enviandoId.set(null);
        this.erroConvite.set(
          e.status === 409
            ? 'Você já sugeriu uma mentoria para essa startup.'
            : mensagemDeErro(e, 'Não foi possível enviar a sugestão. Tente novamente.'),
        );
      },
    });
  }
}
