import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { JanelaModalComponent } from '../../shared/janela-modal/janela-modal.component';
import { AcompanhamentoService } from '../acompanhamento.service';
import { Atividade } from '../acompanhamento.models';

export type ModoFeedback = 'APROVAR' | 'DEVOLVER';

// Janela do mentor para aprovar (feedback opcional) ou devolver (feedback obrigatório) uma entrega.
@Component({
  selector: 'app-modal-feedback',
  standalone: true,
  imports: [FormsModule, JanelaModalComponent],
  template: `
    <app-janela-modal [titulo]="titulo()" [descricao]="descricao()">
      <form ngNoForm class="flex flex-col gap-4" (submit)="$event.preventDefault(); confirmar()">
        @if (atividade(); as a) {
          <p class="rounded-lg bg-gray-50 px-4 py-3 font-medium">{{ a.titulo }}</p>
        }
        <label class="grid gap-1">
          <span class="font-medium">
            Feedback
            @if (modo() === 'APROVAR') {
              <span class="font-normal text-gray-500">(opcional)</span>
            } @else {
              *
            }
          </span>
          <textarea
            rows="4"
            maxlength="1000"
            class="campo"
            [placeholder]="modo() === 'DEVOLVER' ? 'Explique o que precisa ser ajustado.' : 'Deixe um comentário para a startup.'"
            [ngModel]="feedback()"
            (ngModelChange)="feedback.set($event)"
          ></textarea>
        </label>
        @if (erro()) {
          <p class="text-sm text-red-600" role="alert">{{ erro() }}</p>
        }
        <div class="flex flex-wrap justify-end gap-3">
          <button
            type="button"
            (click)="janela().fechar()"
            class="cursor-pointer rounded-lg border border-gray-300 bg-white px-6 py-2.5 font-medium text-gray-800 transition-colors hover:bg-gray-50"
          >
            Cancelar
          </button>
          <button
            type="submit"
            [disabled]="salvando()"
            class="cursor-pointer rounded-lg bg-ponti-roxo px-8 py-2.5 font-medium text-white transition-colors hover:bg-ponti-roxo/90 disabled:cursor-default disabled:bg-gray-200 disabled:text-gray-600"
          >
            {{ salvando() ? 'Enviando...' : modo() === 'APROVAR' ? 'Aprovar entrega' : 'Devolver para ajustes' }}
          </button>
        </div>
      </form>
    </app-janela-modal>
  `,
})
export class ModalFeedbackComponent {
  concluido = output<Atividade>();

  private service = inject(AcompanhamentoService);
  readonly janela = viewChild.required(JanelaModalComponent);

  atividade = signal<Atividade | null>(null);
  modo = signal<ModoFeedback>('APROVAR');
  feedback = signal('');
  salvando = signal(false);
  erro = signal('');

  titulo = computed(() => (this.modo() === 'APROVAR' ? 'Aprovar entrega' : 'Devolver entrega'));
  descricao = computed(() =>
    this.modo() === 'APROVAR'
      ? 'A atividade será marcada como aprovada. O feedback é opcional.'
      : 'A startup poderá reenviar. Explique o que precisa ser ajustado (obrigatório).',
  );

  abrir(atividade: Atividade, modo: ModoFeedback) {
    this.atividade.set(atividade);
    this.modo.set(modo);
    this.feedback.set('');
    this.erro.set('');
    this.janela().abrir();
  }

  confirmar() {
    const atividade = this.atividade();
    if (!atividade || this.salvando()) return;
    const feedback = this.feedback().trim();
    if (this.modo() === 'DEVOLVER' && !feedback) {
      this.erro.set('Explique o que a startup precisa ajustar.');
      return;
    }
    this.salvando.set(true);
    this.erro.set('');
    const chamada =
      this.modo() === 'APROVAR'
        ? this.service.aprovar(atividade.id, feedback || undefined)
        : this.service.devolver(atividade.id, feedback);
    chamada.subscribe({
      next: (atualizada) => {
        this.salvando.set(false);
        this.janela().fechar();
        this.concluido.emit(atualizada);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível salvar. Tente de novo.'));
        this.salvando.set(false);
      },
    });
  }
}
