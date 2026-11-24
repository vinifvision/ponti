import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { IconeComponent } from '../../shared/icone/icone.component';
import { JanelaModalComponent } from '../../shared/janela-modal/janela-modal.component';
import { AcompanhamentoService } from '../acompanhamento.service';

const MAXIMO_METAS = 5;
let proximaMeta = 0;

interface MetaEmEdicao {
  chave: number;
  titulo: string;
}

// Janela do mentor para propor um acompanhamento a uma startup: objetivo e metas iniciais (opcionais).
// A startup recebe a proposta na aba Acompanhamentos e aceita ou recusa.
@Component({
  selector: 'app-modal-proposta',
  standalone: true,
  imports: [FormsModule, IconeComponent, JanelaModalComponent],
  template: `
    <app-janela-modal
      titulo="Propor acompanhamento"
      [descricao]="'Defina o objetivo do acompanhamento com ' + nomeStartup() + '. Ela poderá aceitar ou recusar a proposta.'"
    >
      <form ngNoForm class="flex flex-col gap-5" (submit)="$event.preventDefault(); enviar()">
        <label class="grid gap-1">
          <span class="font-medium">Objetivo *</span>
          <textarea
            rows="3"
            maxlength="300"
            class="campo"
            placeholder="Ex.: Validar o MVP e fechar os 3 primeiros clientes piloto."
            [ngModel]="objetivo()"
            (ngModelChange)="objetivo.set($event)"
          ></textarea>
        </label>

        <fieldset class="grid gap-3">
          <legend class="font-medium">
            Metas iniciais <span class="font-normal text-gray-500">(opcional, até {{ maximo }})</span>
          </legend>
          @for (meta of metas(); track meta.chave; let i = $index) {
            <div class="flex items-center gap-2">
              <input
                maxlength="120"
                class="campo"
                [attr.aria-label]="'Meta ' + (i + 1)"
                placeholder="Ex.: Fechar 3 clientes piloto"
                [(ngModel)]="meta.titulo"
                [ngModelOptions]="{ standalone: true }"
              />
              <button
                type="button"
                (click)="removerMeta(meta.chave)"
                [attr.aria-label]="'Remover meta ' + (i + 1)"
                class="shrink-0 cursor-pointer rounded-lg p-2 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-800"
              >
                <app-icone nome="fechar" [tamanho]="18" />
              </button>
            </div>
          }
          @if (metas().length < maximo) {
            <div>
              <button
                type="button"
                (click)="adicionarMeta()"
                class="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-ponti-lilas px-4 py-2 text-sm font-medium text-gray-800 transition-colors hover:bg-ponti-lilas/20"
              >
                <app-icone nome="mais" [tamanho]="16" /> Adicionar meta
              </button>
            </div>
          }
        </fieldset>

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
            [disabled]="enviando()"
            class="cursor-pointer rounded-lg bg-ponti-roxo px-8 py-2.5 font-medium text-white transition-colors hover:bg-ponti-roxo/90 disabled:cursor-default disabled:bg-gray-200 disabled:text-gray-600"
          >
            {{ enviando() ? 'Enviando...' : 'Enviar proposta' }}
          </button>
        </div>
      </form>
    </app-janela-modal>
  `,
})
export class ModalPropostaComponent {
  startupId = input.required<number>();
  nomeStartup = input('a startup');

  private service = inject(AcompanhamentoService);
  private router = inject(Router);
  readonly janela = viewChild.required(JanelaModalComponent);

  readonly maximo = MAXIMO_METAS;
  objetivo = signal('');
  metas = signal<MetaEmEdicao[]>([]);
  enviando = signal(false);
  erro = signal('');

  abrir() {
    this.objetivo.set('');
    this.metas.set([]);
    this.erro.set('');
    this.janela().abrir();
  }

  adicionarMeta() {
    if (this.metas().length >= MAXIMO_METAS) return;
    this.metas.update((lista) => [...lista, { chave: ++proximaMeta, titulo: '' }]);
  }

  removerMeta(chave: number) {
    this.metas.update((lista) => lista.filter((m) => m.chave !== chave));
  }

  enviar() {
    if (this.enviando()) return;
    const objetivo = this.objetivo().trim();
    if (!objetivo) {
      this.erro.set('Informe o objetivo do acompanhamento.');
      return;
    }
    this.enviando.set(true);
    this.erro.set('');
    const metas = this.metas().map((m) => m.titulo.trim()).filter(Boolean);
    this.service.propor({ startupId: this.startupId(), objetivo, metas, nomeStartup: this.nomeStartup() }).subscribe({
      next: () => {
        this.enviando.set(false);
        this.janela().fechar();
        this.router.navigateByUrl('/acompanhamentos');
      },
      error: (e: HttpErrorResponse) => {
        const padrao =
          e.status === 409
            ? 'Já existe uma proposta ou um acompanhamento em andamento com esta startup.'
            : 'Não foi possível enviar a proposta. Tente de novo.';
        this.erro.set(e.status === 409 ? padrao : mensagemDeErro(e, padrao));
        this.enviando.set(false);
      },
    });
  }
}
