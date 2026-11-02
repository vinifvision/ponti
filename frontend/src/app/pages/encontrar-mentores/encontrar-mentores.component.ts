import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SessaoService } from '../../auth/sessao.service';
import { StartupService } from '../../startups/startup.service';
import { BuscaService } from '../../layout/busca.service';
import { MentorCardComponent } from '../../mentores/mentor-card/mentor-card.component';
import { MentorDestaqueComponent } from '../../mentores/mentor-destaque/mentor-destaque.component';
import { MentorService } from '../../mentores/mentor.service';
import { atendeFaixaExperiencia } from '../../shared/experiencia.utils';
import { atendeFaixaPreco } from '../../shared/preco.utils';
import { Mentor } from '../../models';
import { FiltroSelectComponent, OpcaoFiltro } from '../../shared/filtro-select/filtro-select.component';
import {
  OPCOES_EXPERIENCIA,
  OPCOES_ORDENACAO_MENTORES,
  OPCOES_PRECO,
  opcoesDeArea,
} from '../../shared/busca-opcoes';
import { separarLista } from '../../shared/texto.utils';

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

  mentores = signal<Mentor[]>([]);
  carregando = signal(true);
  erro = signal('');
  // true quando a startup ainda não escolheu os temas (sem eles não há como calcular o match)
  semTemas = signal(false);

  // Os filtros ficam no BuscaService: a navbar (Enter na busca) e esta tela mexem nos mesmos valores
  area = this.busca.area;
  experiencia = this.busca.experiencia;
  faixaPreco = this.busca.faixaPreco;
  ordenacao = this.busca.ordenacaoMentores;
  selecionadoId = signal<number | undefined>(undefined);
  focoId = signal<number | undefined>(undefined);

  readonly opcoesExperiencia: OpcaoFiltro[] = OPCOES_EXPERIENCIA;
  readonly opcoesPreco: OpcaoFiltro[] = OPCOES_PRECO;
  readonly opcoesOrdenacao: OpcaoFiltro[] = OPCOES_ORDENACAO_MENTORES;

  opcoesArea = computed<OpcaoFiltro[]>(() => opcoesDeArea(this.mentores()));

  mentoresFiltrados = computed(() => {
    const termo = this.busca.termo().trim().toLowerCase();
    const area = this.area().toLowerCase();

    const filtrados = this.mentores().filter((m) => {
      const texto = [m.nome, m.bio, m.areasAtuacao, m.ecossistemas].join(' ').toLowerCase();
      const passaBusca = !termo || texto.includes(termo);
      const passaArea = !area || separarLista(m.ecossistemas).some((e) => e.toLowerCase() === area);
      const passaExperiencia = atendeFaixaExperiencia(m.anosExperiencia, this.experiencia());
      const passaPreco = atendeFaixaPreco(m.valorSessao, this.faixaPreco());
      return passaBusca && passaArea && passaExperiencia && passaPreco;
    });

    switch (this.ordenacao()) {
      case 'nome':
        return [...filtrados].sort((a, b) => a.nome.localeCompare(b.nome));
      case 'menor-valor':
        return [...filtrados].sort((a, b) => (a.valorSessao ?? 0) - (b.valorSessao ?? 0));
      case 'maior-valor':
        return [...filtrados].sort((a, b) => (b.valorSessao ?? 0) - (a.valorSessao ?? 0));
      default: // 'compatibilidade' ou vazio: a ordem que o backend mandou (mais compatível primeiro)
        return filtrados;
    }
  });

  ngOnInit() {
    const startupId = this.sessao.startupId();
    if (startupId !== null) {
      this.startupService.buscar(startupId).subscribe({
        next: (s) => this.semTemas.set(!s.necessidades?.length),
        error: () => {}, // sem o aviso a lista continua funcionando
      });
    }
    this.mentorService.recomendados().subscribe({
      next: (dados) => {
        this.mentores.set(dados);
        // A navbar mostra o mesmo filtro de área no painel da busca
        this.busca.opcoesArea.set(this.opcoesArea());
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar os mentores. Verifique se a API está rodando.');
        this.carregando.set(false);
      },
    });
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
