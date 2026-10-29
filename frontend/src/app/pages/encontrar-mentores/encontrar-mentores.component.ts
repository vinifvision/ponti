import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SessaoService } from '../../auth/sessao.service';
import { StartupService } from '../../startups/startup.service';
import { BuscaService } from '../../layout/busca.service';
import { MentorCardComponent } from '../../mentores/mentor-card/mentor-card.component';
import { MentorDestaqueComponent } from '../../mentores/mentor-destaque/mentor-destaque.component';
import { MentorService } from '../../mentores/mentor.service';
import { atendeFaixaExperiencia } from '../../shared/experiencia.utils';
import { FAIXAS_PRECO, atendeFaixaPreco } from '../../shared/preco.utils';
import { Mentor } from '../../models';
import { FiltroSelectComponent, OpcaoFiltro } from '../../shared/filtro-select/filtro-select.component';
import { separarLista } from '../../shared/texto.utils';

@Component({
  selector: 'app-encontrar-mentores',
  standalone: true,
  imports: [FiltroSelectComponent, MentorCardComponent, MentorDestaqueComponent, RouterLink],
  templateUrl: './encontrar-mentores.component.html',
})
export class EncontrarMentoresComponent implements OnInit {
  private mentorService = inject(MentorService);
  private busca = inject(BuscaService);
  private sessao = inject(SessaoService);
  private startupService = inject(StartupService);

  mentores = signal<Mentor[]>([]);
  carregando = signal(true);
  erro = signal('');
  // true quando a startup ainda não escolheu os temas (sem eles não há como calcular o match)
  semTemas = signal(false);

  area = signal('');
  experiencia = signal('');
  faixaPreco = signal('');
  ordenacao = signal('');
  selecionadoId = signal<number | undefined>(undefined);
  focoId = signal<number | undefined>(undefined);

  readonly opcoesExperiencia: OpcaoFiltro[] = [
    { valor: 'iniciante', rotulo: 'Até 2 anos' },
    { valor: 'intermediario', rotulo: '3 a 5 anos' },
    { valor: 'senior', rotulo: 'Mais de 5 anos' },
  ];

  readonly opcoesPreco: OpcaoFiltro[] = FAIXAS_PRECO;

  readonly opcoesOrdenacao: OpcaoFiltro[] = [
    { valor: 'compatibilidade', rotulo: 'Mais compatíveis' },
    { valor: 'nome', rotulo: 'Nome (A-Z)' },
    { valor: 'menor-valor', rotulo: 'Menor valor por sessão' },
    { valor: 'maior-valor', rotulo: 'Maior valor por sessão' },
  ];

  opcoesArea = computed<OpcaoFiltro[]>(() => {
    const areas = new Set(this.mentores().flatMap((m) => separarLista(m.ecossistemas)));
    return [...areas].sort().map((area) => ({ valor: area, rotulo: area }));
  });

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
