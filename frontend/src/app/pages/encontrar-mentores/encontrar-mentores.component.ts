import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { BuscaService } from '../../layout/busca.service';
import { MentorCardComponent } from '../../mentores/mentor-card/mentor-card.component';
import { MentorDestaqueComponent } from '../../mentores/mentor-destaque/mentor-destaque.component';
import { MentorService } from '../../mentores/mentor.service';
import { Mentor } from '../../models';
import { FiltroSelectComponent, OpcaoFiltro } from '../../shared/filtro-select/filtro-select.component';
import { separarLista } from '../../shared/texto.utils';

@Component({
  selector: 'app-encontrar-mentores',
  standalone: true,
  imports: [FiltroSelectComponent, MentorCardComponent, MentorDestaqueComponent],
  templateUrl: './encontrar-mentores.component.html',
})
export class EncontrarMentoresComponent implements OnInit {
  private mentorService = inject(MentorService);
  private busca = inject(BuscaService);

  mentores = signal<Mentor[]>([]);
  carregando = signal(true);
  erro = signal('');

  area = signal('');
  experiencia = signal('');
  ordenacao = signal('');
  selecionadoId = signal<number | undefined>(undefined);
  focoId = signal<number | undefined>(undefined);
  conectados = signal<Set<number>>(new Set());
  // TEMPORÁRIO!!: só pra testar o visual do card, remover quando a API estiver funcionando
  private readonly mentoresMock: Mentor[] = [
  {
    id: 1,
    nome: 'Juliana G.',
    bio: 'Zootecnista e professora, com experiência ajudando startups de Health-techs a validar o produto com usuários reais.',
    areasAtuacao: 'Zootecnista, Professora',
    ecossistemas: 'Health-techs, PetTech',
    valorSessao: 0,
  },
  {
    id: 2,
    nome: 'Roberto',
    bio: 'Veterinário com foco em PetTechs, ajuda startups a validar hipóteses de produto na área de saúde animal.',
    areasAtuacao: 'Veterinário',
    ecossistemas: 'Health-techs, PetTech',
    valorSessao: 0,
  },
];

  readonly opcoesExperiencia: OpcaoFiltro[] = [
    { valor: 'iniciante', rotulo: 'Até 2 anos' },
    { valor: 'intermediario', rotulo: '2 a 5 anos' },
    { valor: 'senior', rotulo: 'Mais de 5 anos' },
  ];

  readonly opcoesOrdenacao: OpcaoFiltro[] = [
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
      return passaBusca && passaArea;
    });

    switch (this.ordenacao()) {
      case 'nome':
        return [...filtrados].sort((a, b) => a.nome.localeCompare(b.nome));
      case 'menor-valor':
        return [...filtrados].sort((a, b) => (a.valorSessao ?? 0) - (b.valorSessao ?? 0));
      case 'maior-valor':
        return [...filtrados].sort((a, b) => (b.valorSessao ?? 0) - (a.valorSessao ?? 0));
      default:
        return filtrados;
    }
  });

  ngOnInit() {
  this.mentorService.listar().subscribe({
    next: (dados) => {
      this.mentores.set(dados);
      this.carregando.set(false);
    },
    error: () => {
      // TEMPORÁRIO!!: só pra testar o visual do card, remover quando a API estiver funcionando
      this.mentores.set(this.mentoresMock);
      this.carregando.set(false);
      },
    });
  }

  ehCompativel(mentor: Mentor): boolean {
    const area = this.area().toLowerCase();
    return !!area && separarLista(mentor.ecossistemas).some((e) => e.toLowerCase() === area);
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
  conectar(mentor: Mentor) {
    if (mentor.id == null) return;
    // Enviar a solicitação de conexão para o backend quando o endpoint existir.
    this.conectados.update((ids) => new Set(ids).add(mentor.id!));
  }
}
