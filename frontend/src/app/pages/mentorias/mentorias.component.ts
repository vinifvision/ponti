import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AgendaService } from '../../agenda/agenda.service';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { ConviteService } from '../../convites/convite.service';
import { Agendamento, ConviteDetalhe, StatusAgendamento } from '../../models';
import { dataHoraCompleta, hora, jaPassou } from '../../shared/data.utils';
import { descreverMaturidade } from '../../shared/maturidade.utils';
import { formatarDuracao, formatarValorSessao, linkSeguro } from '../../shared/mentoria.utils';

type Situacao = 'AGUARDANDO' | 'CONFIRMADA' | 'REALIZADA' | 'RECUSADA' | 'CANCELADA';

// "Mentorias": as sessões agendadas dos dois lados (fluxo estilo Preply) e as sugestões de mentoria.
//  - Startup: sugestões recebidas dos mentores, próximas sessões (com o link do Meet) e histórico.
//  - Mentor: pedidos para confirmar (com o link do Meet), próximas sessões, histórico e sugestões enviadas.
@Component({
  selector: 'app-mentorias',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './mentorias.component.html',
})
export class MentoriasComponent implements OnInit {
  private agendaService = inject(AgendaService);
  private conviteService = inject(ConviteService);
  private sessao = inject(SessaoService);

  ehMentor = computed(() => this.sessao.tipo() === 'MENTOR');
  agendamentos = signal<Agendamento[]>([]);
  sugestoes = signal<ConviteDetalhe[]>([]);
  carregando = signal(true);
  erro = signal('');
  agindoId = signal<number | null>(null);
  // Link do Meet digitado em cada pedido, antes de confirmar
  links: Record<number, string> = {};

  pedidos = computed(() =>
    this.agendamentos().filter((a) => a.status === 'PENDENTE' && !jaPassou(a.inicio)),
  );
  proximas = computed(() =>
    this.agendamentos().filter((a) =>
      this.ehMentor() ? a.status === 'CONFIRMADO' && !jaPassou(a.fim) : ['PENDENTE', 'CONFIRMADO'].includes(a.status) && !jaPassou(a.fim),
    ),
  );
  historico = computed(() => {
    const ativos = new Set([...this.pedidos(), ...this.proximas()].map((a) => a.id));
    return this.agendamentos().filter((a) => !ativos.has(a.id)).reverse();
  });
  sugestoesPendentes = computed(() => this.sugestoes().filter((c) => c.status === 'PENDENTE'));

  ngOnInit() {
    this.carregar();
  }

  carregar() {
    this.carregando.set(true);
    this.erro.set('');
    const mentorId = this.sessao.mentorId();
    const startupId = this.sessao.startupId();

    const agendamentos = mentorId !== null ? this.agendaService.doMentor(mentorId) : startupId !== null ? this.agendaService.daStartup(startupId) : null;
    const sugestoes =
      mentorId !== null
        ? this.conviteService.detalhadosDoMentor(mentorId)
        : startupId !== null
          ? this.conviteService.detalhadosDaStartup(startupId)
          : null;
    if (!agendamentos || !sugestoes) {
      this.carregando.set(false);
      return;
    }

    agendamentos.subscribe({
      next: (lista) => {
        this.agendamentos.set(lista);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar as mentorias.'));
        this.carregando.set(false);
      },
    });
    // Sugestões são os convites que partiram do mentor
    sugestoes.subscribe({
      next: (lista) => this.sugestoes.set(lista.filter((c) => c.enviadoPor === 'MENTOR')),
      error: () => {},
    });
  }

  confirmar(a: Agendamento) {
    const link = (this.links[a.id] ?? '').trim();
    if (!/^https:\/\/\S+$/.test(link)) {
      this.erro.set('Cole o link da reunião do Google Meet (começa com https://) para confirmar.');
      return;
    }
    this.agir(a.id, this.agendaService.confirmar(a.id, link));
  }

  recusar(a: Agendamento) {
    this.agir(a.id, this.agendaService.recusar(a.id));
  }

  cancelar(a: Agendamento) {
    if (!confirm('Cancelar esta mentoria?')) return;
    this.agir(a.id, this.agendaService.cancelar(a.id));
  }

  dispensarSugestao(c: ConviteDetalhe) {
    this.agindoId.set(c.id);
    this.conviteService.recusar(c.id).subscribe({
      next: () => {
        this.agindoId.set(null);
        this.carregar();
      },
      error: (e: HttpErrorResponse) => {
        this.agindoId.set(null);
        this.erro.set(mensagemDeErro(e, 'Não foi possível dispensar a sugestão.'));
      },
    });
  }

  private agir(id: number, requisicao: ReturnType<AgendaService['recusar']>) {
    if (this.agindoId() !== null) return;
    this.agindoId.set(id);
    this.erro.set('');
    requisicao.subscribe({
      next: () => {
        this.agindoId.set(null);
        this.carregar();
      },
      error: (e: HttpErrorResponse) => {
        this.agindoId.set(null);
        this.erro.set(mensagemDeErro(e, 'Não foi possível concluir a ação. Tente novamente.'));
        this.carregar();
      },
    });
  }

  // ---------- textos do template ----------

  quando(a: Agendamento): string {
    return `${dataHoraCompleta(a.inicio)} até ${hora(a.fim)}`;
  }

  outraParte(a: Agendamento) {
    return this.ehMentor() ? a.startup : a.mentor;
  }

  situacao(a: Agendamento): Situacao {
    const mapa: Record<StatusAgendamento, Situacao> = {
      PENDENTE: 'AGUARDANDO',
      CONFIRMADO: jaPassou(a.fim) ? 'REALIZADA' : 'CONFIRMADA',
      RECUSADO: 'RECUSADA',
      CANCELADO: 'CANCELADA',
    };
    return a.status === 'PENDENTE' && jaPassou(a.inicio) ? 'RECUSADA' : mapa[a.status];
  }

  rotulo(a: Agendamento): string {
    const textos: Record<Situacao, string> = {
      AGUARDANDO: 'Aguardando confirmação',
      CONFIRMADA: 'Confirmada',
      REALIZADA: 'Realizada',
      RECUSADA: a.status === 'PENDENTE' ? 'Expirada' : 'Recusada',
      CANCELADA: 'Cancelada',
    };
    return textos[this.situacao(a)];
  }

  classe(a: Agendamento): string {
    const classes: Record<Situacao, string> = {
      AGUARDANDO: 'bg-ponti-amarelo/30 text-ponti-marinho',
      CONFIRMADA: 'bg-green-100 text-green-800',
      REALIZADA: 'bg-ponti-lilas/40 text-ponti-marinho',
      RECUSADA: 'bg-red-100 text-red-700',
      CANCELADA: 'bg-gray-200 text-gray-700',
    };
    return classes[this.situacao(a)];
  }

  pagamento(a: Agendamento): string {
    const valor = formatarValorSessao(a.valor);
    const metodo = a.metodoPagamento === 'CARTAO' ? 'cartão' : 'Pix';
    switch (a.statusPagamento) {
      case 'GRATUITA':
        return 'Mentoria gratuita';
      case 'PAGO':
        return `${valor} pago via ${metodo} (simulado)`;
      case 'REEMBOLSADO':
        return `${valor} reembolsado (simulado)`;
    }
  }

  link(a: Agendamento): string | null {
    return linkSeguro(a.linkReuniao);
  }

  podeCancelar(a: Agendamento): boolean {
    return (a.status === 'PENDENTE' || a.status === 'CONFIRMADO') && !jaPassou(a.inicio);
  }

  valorDoMentor(c: ConviteDetalhe): string {
    return `${formatarValorSessao(c.mentor?.valorSessao)} por sessão de ${formatarDuracao(c.mentor?.duracaoSessaoMinutos ?? 60)}`;
  }

  maturidade(c: ConviteDetalhe): string {
    return descreverMaturidade(c.startup?.estagioMaturidade);
  }

  rotuloSugestao(c: ConviteDetalhe): string {
    return { PENDENTE: 'Aguardando a startup', ACEITO: 'A startup agendou', RECUSADO: 'Dispensada' }[c.status];
  }
}
