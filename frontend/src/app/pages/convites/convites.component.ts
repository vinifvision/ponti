import { NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { ConviteService } from '../../convites/convite.service';
import { ConviteDetalhe, StatusConvite } from '../../models';
import { descreverMaturidade } from '../../shared/maturidade.utils';
import { formatarDuracao, formatarValorSessao, linkSeguro, linkWhatsapp } from '../../shared/mentoria.utils';

// Ordem das listas: pendentes primeiro, depois aceitos, por último recusados
const ORDEM: Record<StatusConvite, number> = { PENDENTE: 0, ACEITO: 1, RECUSADO: 2 };

// Uma tela para os dois lados, com duas listas:
//  - Recebidos: convites que o outro lado mandou; aqui se aceita ou recusa;
//  - Enviados: convites que você mandou, com a resposta de cada um.
// O convite pode partir da startup ou do mentor (campo enviadoPor).
@Component({
  selector: 'app-convites',
  standalone: true,
  imports: [NgTemplateOutlet, RouterLink],
  templateUrl: './convites.component.html',
})
export class ConvitesComponent implements OnInit {
  private service = inject(ConviteService);
  private sessao = inject(SessaoService);

  ehMentor = computed(() => this.sessao.tipo() === 'MENTOR');
  convites = signal<ConviteDetalhe[]>([]);
  carregando = signal(true);
  erro = signal('');
  respondendoId = signal<number | null>(null);

  private meuLado = computed(() => (this.ehMentor() ? 'MENTOR' : 'STARTUP'));
  recebidos = computed(() => this.ordenar(this.convites().filter((c) => c.enviadoPor !== this.meuLado())));
  enviados = computed(() => this.ordenar(this.convites().filter((c) => c.enviadoPor === this.meuLado())));
  pendentesRecebidos = computed(() => this.recebidos().filter((c) => c.status === 'PENDENTE').length);

  ngOnInit() {
    this.carregar();
  }

  carregar() {
    this.carregando.set(true);
    this.erro.set('');

    const mentorId = this.sessao.mentorId();
    const startupId = this.sessao.startupId();
    const consulta =
      this.ehMentor() && mentorId !== null
        ? this.service.detalhadosDoMentor(mentorId)
        : startupId !== null
          ? this.service.detalhadosDaStartup(startupId)
          : null;

    if (!consulta) {
      this.carregando.set(false);
      return;
    }

    consulta.subscribe({
      next: (lista) => {
        this.convites.set(lista);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar os convites.'));
        this.carregando.set(false);
      },
    });
  }

  responder(convite: ConviteDetalhe, aceitar: boolean) {
    if (this.respondendoId() !== null) return;
    this.respondendoId.set(convite.id);
    this.erro.set('');

    const requisicao = aceitar ? this.service.aceitar(convite.id) : this.service.recusar(convite.id);
    requisicao.subscribe({
      next: () => {
        this.respondendoId.set(null);
        this.carregar();
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível responder ao convite.'));
        this.respondendoId.set(null);
      },
    });
  }

  // ---------- textos usados no template ----------

  valor(c: ConviteDetalhe): string {
    return formatarValorSessao(c.mentor?.valorSessao);
  }

  duracao(c: ConviteDetalhe): string {
    return formatarDuracao(c.mentor?.duracaoSessaoMinutos);
  }

  maturidade(c: ConviteDetalhe): string {
    return descreverMaturidade(c.startup?.estagioMaturidade);
  }

  whatsapp(c: ConviteDetalhe): string | null {
    return linkWhatsapp(c.mentor?.whatsapp);
  }

  linkedin(c: ConviteDetalhe): string | null {
    return linkSeguro(c.mentor?.linkedinUrl);
  }

  site(c: ConviteDetalhe): string | null {
    return linkSeguro(c.startup?.site);
  }

  rotulo(status: StatusConvite): string {
    return { PENDENTE: 'Pendente', ACEITO: 'Aceito', RECUSADO: 'Recusado' }[status];
  }

  classeStatus(status: StatusConvite): string {
    return {
      PENDENTE: 'bg-ponti-amarelo/30 text-ponti-marinho',
      ACEITO: 'bg-green-100 text-green-800',
      RECUSADO: 'bg-red-100 text-red-700',
    }[status];
  }

  private ordenar(lista: ConviteDetalhe[]): ConviteDetalhe[] {
    return [...lista].sort((a, b) => ORDEM[a.status] - ORDEM[b.status] || b.id - a.id);
  }
}
