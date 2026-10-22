import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { ConviteService } from '../../convites/convite.service';
import { ConviteDetalhe, StatusConvite } from '../../models';

// Uma tela para os dois lados:
//  - mentor: convites recebidos, com os botões Aceitar e Recusar;
//  - startup: convites enviados, com o status de cada um.
@Component({
  selector: 'app-convites',
  standalone: true,
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
}
