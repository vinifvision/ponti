import { Component, ElementRef, input, output, viewChild } from '@angular/core';
import { IconeComponent } from '../icone/icone.component';

let proximoId = 0;

// Janela modal para formulários (usa o <dialog> nativo: o navegador cuida do foco preso e do Esc).
// Uso: <app-janela-modal #janela titulo="..."> ...conteúdo... </app-janela-modal> e janela.abrir() / janela.fechar().
// Para só perguntar "tem certeza?", use o DialogoConfirmacaoComponent.
@Component({
  selector: 'app-janela-modal',
  standalone: true,
  imports: [IconeComponent],
  template: `
    <dialog
      #dialogo
      (close)="fechou.emit()"
      (click)="aoClicarFora($event)"
      [attr.aria-labelledby]="idTitulo"
      class="m-auto max-h-[90dvh] w-[min(94vw,36rem)] rounded-3xl border border-ponti-lilas bg-white p-0 text-gray-900 shadow-xl backdrop:bg-black/40"
    >
      <div class="p-8">
        <div class="flex items-start justify-between gap-4">
          <h2 [id]="idTitulo" class="text-2xl font-semibold text-ponti-marinho">{{ titulo() }}</h2>
          <button
            type="button"
            (click)="fechar()"
            aria-label="Fechar"
            class="-mr-2 -mt-1 cursor-pointer rounded-lg p-2 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-marinho"
          >
            <app-icone nome="fechar" [tamanho]="20" />
          </button>
        </div>
        @if (descricao()) {
          <p class="mt-2 text-gray-600">{{ descricao() }}</p>
        }
        <div class="mt-6"><ng-content /></div>
      </div>
    </dialog>
  `,
})
export class JanelaModalComponent {
  titulo = input.required<string>();
  descricao = input('');
  // Avisa sempre que a janela fecha (botão, Esc, clique fora ou fechar() do código)
  fechou = output<void>();

  readonly idTitulo = `janela-modal-${++proximoId}`;
  private dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  abrir() {
    this.dialogo().nativeElement.showModal();
  }

  fechar() {
    const el = this.dialogo().nativeElement;
    if (el.open) el.close();
  }

  // Clique no fundo escuro (fora da janela)
  aoClicarFora(evento: MouseEvent) {
    if (evento.target === this.dialogo().nativeElement) this.fechar();
  }
}
