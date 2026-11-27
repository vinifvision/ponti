import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { IconeComponent } from '../../shared/icone/icone.component';
import { iniciais } from '../../shared/texto.utils';
import { AcompanhamentoService } from '../acompanhamento.service';
import { Atividade, Comentario } from '../acompanhamento.models';
import { dataHoraCurta } from '../acompanhamento.utils';

let proximoId = 0;

// Conversa sobre uma entrega. Fica recolhida (só o título com a setinha) e abre ao clicar.
@Component({
  selector: 'app-comentarios-entrega',
  standalone: true,
  imports: [FormsModule, IconeComponent],
  template: `
    <button
      type="button"
      (click)="alternar()"
      [attr.aria-expanded]="aberto()"
      [attr.aria-controls]="idPainel"
      class="inline-flex cursor-pointer items-center gap-2 rounded-lg py-1 text-sm font-medium text-gray-700 transition-colors hover:text-ponti-marinho focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-marinho"
    >
      <app-icone nome="mensagem" [tamanho]="16" />
      Comentários ({{ comentarios().length }})
      <span class="inline-flex transition-transform duration-200" [class.rotate-180]="aberto()">
        <app-icone nome="seta-baixo" [tamanho]="16" />
      </span>
    </button>

    @if (aberto()) {
      <div [id]="idPainel" class="mt-3 flex flex-col gap-4">
        @if (comentarios().length) {
          <ul class="flex flex-col gap-3">
            @for (c of comentarios(); track c.id) {
              <li class="flex gap-3">
                <div
                  class="flex size-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold"
                  [class]="c.autor === 'MENTOR' ? 'bg-ponti-lilas/40 text-ponti-marinho' : 'bg-gray-200 text-gray-700'"
                  aria-hidden="true"
                >
                  {{ iniciais(c.nomeAutor) }}
                </div>
                <div class="min-w-0 flex-1 rounded-lg px-3 py-2" [class]="c.autor === 'MENTOR' ? 'bg-ponti-lilas/10' : 'bg-gray-50'">
                  <p class="text-sm">
                    <span class="font-semibold">{{ c.nomeAutor }}</span>
                    <span class="text-gray-500"> · {{ c.autor === 'MENTOR' ? 'Mentor' : 'Startup' }} · {{ dataHoraCurta(c.criadoEm) }}</span>
                  </p>
                  <p class="mt-1 whitespace-pre-line break-words">{{ c.texto }}</p>
                </div>
              </li>
            }
          </ul>
        } @else {
          <p class="text-sm text-gray-500">Ainda não há comentários nesta entrega.</p>
        }

        @if (podeComentar()) {
          <div class="flex flex-col gap-2">
            <label class="sr-only" [for]="idPainel + '-texto'">Escreva um comentário</label>
            <textarea
              [id]="idPainel + '-texto'"
              rows="2"
              maxlength="1000"
              placeholder="Escreva um comentário..."
              class="campo"
              [ngModel]="rascunho()"
              (ngModelChange)="rascunho.set($event)"
            ></textarea>
            @if (erro()) {
              <p class="text-sm text-red-600" role="alert">{{ erro() }}</p>
            }
            <div>
              <button
                type="button"
                (click)="enviar()"
                [disabled]="enviando() || !rascunho().trim()"
                class="cursor-pointer rounded-lg bg-ponti-roxo px-6 py-2 text-sm font-medium text-white transition-colors hover:bg-ponti-roxo/90 disabled:cursor-default disabled:bg-gray-200 disabled:text-gray-600"
              >
                {{ enviando() ? 'Enviando...' : 'Comentar' }}
              </button>
            </div>
          </div>
        }
      </div>
    }
  `,
})
export class ComentariosEntregaComponent {
  atividadeId = input.required<number>();
  entregaId = input.required<number>();
  comentarios = input.required<Comentario[]>();
  // Falso quando o acompanhamento está encerrado (somente leitura)
  podeComentar = input(true);
  atualizada = output<Atividade>();

  private service = inject(AcompanhamentoService);

  aberto = signal(false);
  rascunho = signal('');
  enviando = signal(false);
  erro = signal('');

  readonly idPainel = `comentarios-${++proximoId}`;
  readonly iniciais = iniciais;
  readonly dataHoraCurta = dataHoraCurta;

  alternar() {
    this.aberto.update((v) => !v);
  }

  enviar() {
    const texto = this.rascunho().trim();
    if (!texto || this.enviando()) return;
    this.enviando.set(true);
    this.erro.set('');
    this.service.comentar(this.atividadeId(), this.entregaId(), texto).subscribe({
      next: (atividade) => {
        this.rascunho.set('');
        this.enviando.set(false);
        this.atualizada.emit(atividade);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível enviar o comentário.'));
        this.enviando.set(false);
      },
    });
  }
}
