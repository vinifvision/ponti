import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { lerDataHora } from '../../shared/data.utils';
import { IconeComponent } from '../../shared/icone/icone.component';
import { JanelaModalComponent } from '../../shared/janela-modal/janela-modal.component';
import { AcompanhamentoService } from '../acompanhamento.service';
import { Atividade } from '../acompanhamento.models';
import { ACCEPT_ARQUIVOS, MAX_ARQUIVOS, formatarTamanho, juntarArquivos } from '../anexos.utils';
import { ChipAnexoComponent } from './chip-anexo.component';

const dois = (n: number) => String(n).padStart(2, '0');
// Date -> "2026-11-19T23:59", o formato do campo de data e hora do navegador
const paraCampo = (d: Date) =>
  `${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())}T${dois(d.getHours())}:${dois(d.getMinutes())}`;

// Janela do mentor para criar uma atividade dentro de um acompanhamento que já existe.
// Ele pode anexar arquivos de apoio (modelos, guias...), que a startup vê junto da atividade.
@Component({
  selector: 'app-modal-atividade',
  standalone: true,
  imports: [FormsModule, IconeComponent, JanelaModalComponent, ChipAnexoComponent],
  template: `
    <app-janela-modal
      titulo="Nova atividade"
      descricao="A startup vai ver a atividade na linha do tempo e poderá enviar a entrega até o prazo."
    >
      <form ngNoForm class="flex flex-col gap-4" (submit)="$event.preventDefault(); salvar()">
        <label class="grid gap-1">
          <span class="font-medium">Título *</span>
          <input
            maxlength="120"
            class="campo"
            placeholder="Ex.: Mapear 10 entrevistas com clientes"
            [ngModel]="titulo()"
            (ngModelChange)="titulo.set($event)"
          />
        </label>
        <label class="grid gap-1">
          <span class="font-medium">O que a startup precisa fazer</span>
          <textarea
            rows="4"
            maxlength="1000"
            class="campo"
            [ngModel]="descricao()"
            (ngModelChange)="descricao.set($event)"
          ></textarea>
        </label>
        <label class="grid gap-1">
          <span class="font-medium">Prazo de entrega *</span>
          <input
            type="datetime-local"
            class="campo"
            [min]="minimo()"
            [ngModel]="prazo()"
            (ngModelChange)="prazo.set($event)"
          />
        </label>
        <div class="grid gap-2">
          <span class="font-medium">
            Anexos <span class="font-normal text-gray-500">(opcional, até {{ maximoArquivos }}, 10 MB cada)</span>
          </span>
          @if (arquivos().length) {
            <ul class="flex flex-wrap gap-3" aria-label="Arquivos escolhidos">
              @for (arq of arquivos(); track arq) {
                <li class="max-w-full">
                  <app-chip-anexo [nome]="arq.name" [detalhe]="formatarTamanho(arq.size)" [removivel]="true" (remover)="removerArquivo(arq)" />
                </li>
              }
            </ul>
          }
          @if (arquivos().length < maximoArquivos) {
            <div>
              <button
                type="button"
                (click)="entradaArquivo.click()"
                class="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-ponti-lilas px-4 py-2 text-sm font-medium text-gray-800 transition-colors hover:bg-ponti-lilas/20"
              >
                <app-icone nome="mais" [tamanho]="16" /> Anexar arquivo
              </button>
            </div>
          }
          <input #entradaArquivo type="file" multiple class="hidden" [accept]="aceitar" (change)="aoEscolherArquivos($event)" tabindex="-1" aria-hidden="true" />
        </div>
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
            {{ salvando() ? 'Criando...' : 'Criar atividade' }}
          </button>
        </div>
      </form>
    </app-janela-modal>
  `,
})
export class ModalAtividadeComponent {
  acompanhamentoId = input.required<number>();
  criada = output<Atividade>();

  private service = inject(AcompanhamentoService);
  readonly janela = viewChild.required(JanelaModalComponent);

  titulo = signal('');
  descricao = signal('');
  prazo = signal('');
  minimo = signal('');
  arquivos = signal<File[]>([]);
  salvando = signal(false);
  erro = signal('');

  readonly aceitar = ACCEPT_ARQUIVOS;
  readonly maximoArquivos = MAX_ARQUIVOS;
  readonly formatarTamanho = formatarTamanho;

  abrir() {
    const agora = new Date();
    const padrao = new Date(agora);
    padrao.setDate(padrao.getDate() + 7);
    padrao.setHours(23, 59, 0, 0);
    this.titulo.set('');
    this.descricao.set('');
    this.prazo.set(paraCampo(padrao));
    this.minimo.set(paraCampo(agora));
    this.arquivos.set([]);
    this.erro.set('');
    this.janela().abrir();
  }

  aoEscolherArquivos(evento: Event) {
    const entrada = evento.target as HTMLInputElement;
    const resultado = juntarArquivos(this.arquivos(), Array.from(entrada.files ?? []));
    this.arquivos.set(resultado.aceitos);
    this.erro.set(resultado.erro);
    entrada.value = ''; // permite escolher o mesmo arquivo de novo depois de removê-lo
  }

  removerArquivo(arquivo: File) {
    this.arquivos.update((lista) => lista.filter((a) => a !== arquivo));
    this.erro.set('');
  }

  salvar() {
    if (this.salvando()) return;
    const titulo = this.titulo().trim();
    if (!titulo) {
      this.erro.set('Informe o título da atividade.');
      return;
    }
    if (!this.prazo() || lerDataHora(this.prazo()) <= new Date()) {
      this.erro.set('Escolha um prazo que ainda não passou.');
      return;
    }
    this.salvando.set(true);
    this.erro.set('');
    const prazo = `${this.prazo()}:00`;
    this.service
      .criarAtividade(this.acompanhamentoId(), { titulo, descricao: this.descricao(), prazo, arquivos: this.arquivos() })
      .subscribe({
        next: (atividade) => {
          this.salvando.set(false);
          this.janela().fechar();
          this.criada.emit(atividade);
        },
        error: (e: HttpErrorResponse) => {
          this.erro.set(mensagemDeErro(e, 'Não foi possível criar a atividade. Tente de novo.'));
          this.salvando.set(false);
        },
      });
  }
}
