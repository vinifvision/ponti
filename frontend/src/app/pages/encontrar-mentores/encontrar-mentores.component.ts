import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
import { SessaoService } from '../../auth/sessao.service';
import { StartupService } from '../../startups/startup.service';
import { BuscaService } from '../../layout/busca.service';
import { MentorCardComponent } from '../../mentores/mentor-card/mentor-card.component';
import { MentorDestaqueComponent } from '../../mentores/mentor-destaque/mentor-destaque.component';
import { FiltrosMentores, MentorService } from '../../mentores/mentor.service';
import { Mentor, Pagina } from '../../models';
import { FiltroSelectComponent, OpcaoFiltro } from '../../shared/filtro-select/filtro-select.component';
import {
  OPCOES_EXPERIENCIA,
  OPCOES_ORDENACAO_MENTORES,
  OPCOES_PRECO,
  ROTULO_RECOMENDADOS,
} from '../../shared/busca-opcoes';

// Mentores por página na tela
export const TAMANHO_PAGINA = 12;

// O que a tela pediu ao servidor e o que voltou. A "chave" identifica a consulta (filtros + página):
// enquanto a última resposta não for da consulta atual, a tela está carregando.
interface Resposta {
  chave: string;
  pagina?: Pagina<Mentor>;
  falhou?: boolean;
}

// Busca de mentores da startup: os filtros, a ordenação e a paginação são feitos no servidor
// (GET /mentores/recomendados). A tela só monta a consulta e mostra a página que voltou.
@Component({
  selector: 'app-encontrar-mentores',
  standalone: true,
  imports: [FiltroSelectComponent, MentorCardComponent, MentorDestaqueComponent, RouterLink],
  templateUrl: './encontrar-mentores.component.html',
})
export class EncontrarMentoresComponent implements OnInit {
  private mentorService = inject(MentorService);
  readonly busca = inject(BuscaService);
  private sessao = inject(SessaoService);
  private startupService = inject(StartupService);

  // true quando a startup ainda não escolheu os temas (sem eles não há como calcular o match)
  semTemas = signal(false);

  // Os filtros ficam no BuscaService: a navbar (Enter na busca) e esta tela mexem nos mesmos valores
  area = this.busca.area;
  experiencia = this.busca.experiencia;
  faixaPreco = this.busca.faixaPreco;
  comHorarioEm7Dias = this.busca.comHorarioEm7Dias;
  ordenacao = this.busca.ordenacaoMentores;
  selecionadoId = signal<number | undefined>(undefined);
  focoId = signal<number | undefined>(undefined);

  readonly opcoesArea = this.busca.opcoesArea;
  readonly opcoesExperiencia: OpcaoFiltro[] = OPCOES_EXPERIENCIA;
  readonly opcoesPreco: OpcaoFiltro[] = OPCOES_PRECO;
  readonly opcoesOrdenacao: OpcaoFiltro[] = OPCOES_ORDENACAO_MENTORES;
  readonly rotuloRecomendados = ROTULO_RECOMENDADOS;

  // ---- Consulta ao servidor ----

  readonly filtros = computed<FiltrosMentores>(() => ({
    q: this.busca.termo().trim(),
    area: this.area(),
    experiencia: this.experiencia(),
    faixaPreco: this.faixaPreco(),
    comHorarioEm7Dias: this.comHorarioEm7Dias(),
    ordenacao: this.ordenacao(),
  }));
  private chaveFiltros = computed(() => JSON.stringify(this.filtros()));

  // A página escolhida vale só para os filtros em que foi escolhida: mudou um filtro, volta para a primeira
  private paginaEscolhida = signal({ filtros: '', numero: 0 });
  readonly pagina = computed(() =>
    this.paginaEscolhida().filtros === this.chaveFiltros() ? this.paginaEscolhida().numero : 0,
  );

  private consulta = computed(() => ({ filtros: this.filtros(), pagina: this.pagina() }));
  private chaveConsulta = computed(() => JSON.stringify(this.consulta()));

  private resposta = signal<Resposta | null>(null);
  // A última página que chegou: continua na tela (apagada) enquanto a próxima carrega
  private ultimaPagina = signal<Pagina<Mentor> | null>(null);

  readonly carregando = computed(() => this.resposta()?.chave !== this.chaveConsulta());
  readonly erro = computed(() =>
    !this.carregando() && this.resposta()?.falhou
      ? 'Não foi possível carregar os mentores. Verifique se a API está rodando.'
      : '',
  );
  readonly mentores = computed(() => this.ultimaPagina()?.itens ?? []);
  readonly totalItens = computed(() => this.ultimaPagina()?.totalItens ?? 0);
  readonly totalPaginas = computed(() => this.ultimaPagina()?.totalPaginas ?? 0);
  readonly temAnterior = computed(() => this.pagina() > 0);
  readonly temProxima = computed(() => this.pagina() + 1 < this.totalPaginas());

  constructor() {
    // Cada mudança de filtro ou de página faz uma consulta nova; switchMap cancela a anterior,
    // para uma resposta atrasada não sobrescrever a mais recente.
    toObservable(this.consulta)
      .pipe(
        switchMap((c) => {
          const chave = JSON.stringify(c);
          return this.mentorService.buscarRecomendados(c.filtros, c.pagina, TAMANHO_PAGINA).pipe(
            map((pagina): Resposta => ({ chave, pagina })),
            catchError(() => of<Resposta>({ chave, falhou: true })),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((r) => {
        if (r.pagina) {
          this.ultimaPagina.set(r.pagina);
        }
        this.resposta.set(r);
      });
  }

  ngOnInit() {
    const startupId = this.sessao.startupId();
    if (startupId !== null) {
      this.startupService.buscar(startupId).subscribe({
        next: (s) => this.semTemas.set(!s.necessidades?.length),
        error: () => {}, // sem o aviso a lista continua funcionando
      });
    }
    // Todas as áreas (não só as da página atual). A navbar mostra o mesmo filtro no painel da busca.
    if (this.opcoesArea().length === 0) {
      this.mentorService.areas().subscribe({
        next: (areas) => this.busca.opcoesArea.set(areas.map((a) => ({ valor: a, rotulo: a }))),
        error: () => {}, // sem as opções o filtro de área só fica vazio
      });
    }
  }

  irParaPagina(numero: number) {
    this.paginaEscolhida.set({ filtros: this.chaveFiltros(), numero });
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  selecionar(mentor: Mentor) {
    this.selecionadoId.set(mentor.id);
  }

  focar(mentor: Mentor) {
    this.focoId.set(mentor.id);
  }

  desfocarSe(mentor: Mentor) {
    if (this.focoId() === mentor.id) {
      this.focoId.set(undefined);
    }
  }
}
