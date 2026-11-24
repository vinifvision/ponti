import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, output, signal, viewChild } from '@angular/core';
import { AgendaService } from '../../agenda/agenda.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { SessaoService } from '../../auth/sessao.service';
import { dataCurta } from '../acompanhamento.utils';
import { IconeComponent } from '../../shared/icone/icone.component';
import { JanelaModalComponent } from '../../shared/janela-modal/janela-modal.component';
import { iniciais } from '../../shared/texto.utils';
import { AcompanhamentoService } from '../acompanhamento.service';
import { StartupElegivel, startupsElegiveis } from '../elegibilidade.utils';
import { ModalPropostaComponent } from './modal-proposta.component';

// Ação do mentor "Novo acompanhamento": mostra só as startups elegíveis (já tiveram mentoria com ele e ainda não
// têm proposta nem acompanhamento em andamento). Ao escolher uma, abre a janela de proposta (objetivo e metas).
// Uso: <app-modal-novo-acompanhamento #novo (enviada)="..." /> e novo.abrir() num botão
// (hoje, o botão "Novo acompanhamento" da tela principal de Acompanhamentos).
@Component({
  selector: 'app-modal-novo-acompanhamento',
  standalone: true,
  imports: [IconeComponent, JanelaModalComponent, ModalPropostaComponent],
  template: `
    <app-janela-modal
      #janelaLista
      titulo="Novo acompanhamento"
      descricao="Escolha uma startup com a qual você já fez mentoria. Ela recebe a proposta e pode aceitar ou recusar."
    >
      @if (carregando()) {
        <p class="text-gray-500" role="status">Buscando startups...</p>
      } @else if (erro()) {
        <p class="rounded-lg bg-red-50 p-3 text-red-700" role="alert">{{ erro() }}</p>
      } @else if (!elegiveis().length) {
        <div class="rounded-xl border border-dashed border-gray-300 p-6 text-center">
          <p class="font-medium">Nenhuma startup disponível para propor agora</p>
          <p class="mt-2 text-sm text-gray-600">
            Aparecem aqui as startups com quem você já teve uma mentoria realizada e que ainda não têm proposta pendente
            nem acompanhamento ativo com você.
          </p>
        </div>
      } @else {
        <ul class="flex max-h-[50dvh] flex-col gap-2 overflow-y-auto pr-1" aria-label="Startups elegíveis">
          @for (s of elegiveis(); track s.id) {
            <li>
              <button
                type="button"
                (click)="escolher(s)"
                class="flex w-full cursor-pointer items-center gap-3 rounded-xl border border-gray-200 bg-white p-3 text-left transition-colors hover:border-ponti-lilas hover:bg-ponti-lilas/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-marinho"
              >
                @if (s.fotoUrl) {
                  <img [src]="s.fotoUrl" alt="" class="size-11 shrink-0 rounded-full border border-gray-200 object-cover" />
                } @else {
                  <span class="flex size-11 shrink-0 items-center justify-center rounded-full bg-ponti-lilas/40 font-semibold text-ponti-marinho" aria-hidden="true">
                    {{ iniciais(s.nome) }}
                  </span>
                }
                <span class="min-w-0 flex-1">
                  <span class="block truncate font-bold">{{ s.nome }}</span>
                  @if (s.descricao) {
                    <span class="block truncate text-sm text-gray-600">{{ s.descricao }}</span>
                  }
                  <span class="block text-sm text-gray-500">
                    {{ s.mentoriasRealizadas }} {{ s.mentoriasRealizadas === 1 ? 'mentoria realizada' : 'mentorias realizadas' }} ·
                    última em {{ dataCurta(s.ultimaMentoria) }}
                  </span>
                </span>
                <app-icone nome="seta-esquerda" [tamanho]="20" class="shrink-0 rotate-180 text-gray-400" />
              </button>
            </li>
          }
        </ul>
      }
    </app-janela-modal>

    <app-modal-proposta
      #proposta
      [startupId]="escolhida()?.id ?? 0"
      [nomeStartup]="escolhida()?.nome ?? 'a startup'"
      (enviada)="enviada.emit()"
    />
  `,
})
export class ModalNovoAcompanhamentoComponent {
  // A proposta foi enviada (a tela de Acompanhamentos recarrega a lista)
  enviada = output<void>();

  private agenda = inject(AgendaService);
  private service = inject(AcompanhamentoService);
  private sessao = inject(SessaoService);

  private janelaLista = viewChild.required<JanelaModalComponent>('janelaLista');
  private proposta = viewChild.required<ModalPropostaComponent>('proposta');

  readonly iniciais = iniciais;
  readonly dataCurta = dataCurta;

  carregando = signal(false);
  erro = signal('');
  elegiveis = signal<StartupElegivel[]>([]);
  escolhida = signal<StartupElegivel | null>(null);

  // Busca de novo a cada abertura: mentorias e acompanhamentos podem ter mudado
  abrir() {
    this.erro.set('');
    this.elegiveis.set([]);
    this.janelaLista().abrir();

    const mentorId = this.sessao.mentorId();
    if (mentorId === null) {
      this.erro.set('Só mentores podem propor acompanhamentos.');
      return;
    }
    this.carregando.set(true);
    this.agenda.doMentor(mentorId).subscribe({
      next: (agendamentos) =>
        this.service.listar().subscribe({
          next: (acompanhamentos) => {
            this.elegiveis.set(startupsElegiveis(agendamentos, acompanhamentos));
            this.carregando.set(false);
          },
          error: (e: HttpErrorResponse) => this.falhar(e),
        }),
      error: (e: HttpErrorResponse) => this.falhar(e),
    });
  }

  escolher(startup: StartupElegivel) {
    this.escolhida.set(startup);
    this.janelaLista().fechar();
    // Espera a tela atualizar o nome da startup antes de abrir a janela de proposta
    setTimeout(() => this.proposta().abrir());
  }

  private falhar(e: HttpErrorResponse) {
    this.erro.set(mensagemDeErro(e, 'Não foi possível buscar as startups. Tente de novo.'));
    this.carregando.set(false);
  }
}
