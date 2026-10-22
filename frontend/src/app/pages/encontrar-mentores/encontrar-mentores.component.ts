import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { SessaoService } from '../../auth/sessao.service';
import { ConviteService } from '../../convites/convite.service';
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
  private conviteService = inject(ConviteService);
  private sessao = inject(SessaoService);
  private busca = inject(BuscaService);

  mentores = signal<Mentor[]>([]);
  carregando = signal(true);
  erro = signal('');
  erroConexao = signal('');

  area = signal('');
  experiencia = signal('');
  ordenacao = signal('');
  selecionadoId = signal<number | undefined>(undefined);
  focoId = signal<number | undefined>(undefined);
  conectados = signal<Set<number>>(new Set());

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
        this.erro.set('Não foi possível carregar os mentores. Verifique se a API está rodando.');
        this.carregando.set(false);
      },
    });

    // Marca como "Solicitação enviada" os mentores que essa startup já convidou.
    const startupId = this.sessao.startupId();
    if (startupId !== null) {
      this.conviteService.listarEnviados(startupId).subscribe({
        next: (convites) => this.conectados.set(new Set(convites.map((c) => c.mentorId))),
        // Se falhar, a lista de mentores continua funcionando: o backend recusa convite duplicado (409).
        error: () => {},
      });
    }
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
    const startupId = this.sessao.startupId();
    if (mentor.id == null || startupId === null) return; // só startup envia convite
    const mentorId = mentor.id;

    this.erroConexao.set('');
    // Atualiza o botão na hora; se a API falhar, desfaz abaixo.
    this.conectados.update((ids) => new Set(ids).add(mentorId));

    this.conviteService.criar({ startupId, mentorId }).subscribe({
      error: (e: HttpErrorResponse) => {
        if (e.status === 409) return; // esse convite já existia, então o botão continua "enviado"

        this.conectados.update((ids) => {
          const restantes = new Set(ids);
          restantes.delete(mentorId);
          return restantes;
        });
        this.erroConexao.set(e.error?.message ?? 'Não foi possível enviar a solicitação. Tente novamente.');
      },
    });
  }
}
