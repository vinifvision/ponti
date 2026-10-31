import { Component, ElementRef, input, viewChild } from '@angular/core';

// Janela de confirmação (usa o <dialog> nativo: o navegador cuida do foco preso dentro da janela e do Esc).
// Uso: ter o componente na tela e chamar `await dialogo.perguntar()`; resolve true (confirmou) ou false (cancelou).
@Component({
  selector: 'app-dialogo-confirmacao',
  standalone: true,
  template: `
    <dialog
      #dialogo
      (cancel)="aoCancelar($event)"
      (click)="aoClicarFora($event)"
      aria-labelledby="dialogo-confirmacao-titulo"
      aria-describedby="dialogo-confirmacao-mensagem"
      class="m-auto w-[min(92vw,30rem)] rounded-3xl border border-ponti-lilas bg-white p-0 text-gray-900 shadow-xl backdrop:bg-black/40"
    >
      <div class="p-8">
        <h2 id="dialogo-confirmacao-titulo" class="text-2xl font-semibold text-ponti-marinho">{{ titulo() }}</h2>
        <p id="dialogo-confirmacao-mensagem" class="mt-3 text-lg text-gray-700">{{ mensagem() }}</p>

        <div class="mt-8 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            (click)="responder(true)"
            class="cursor-pointer rounded-lg border border-gray-300 px-6 py-3 font-medium text-gray-800 transition hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-marinho"
          >
            {{ textoConfirmar() }}
          </button>
          <button
            type="button"
            autofocus
            (click)="responder(false)"
            class="cursor-pointer rounded-lg bg-ponti-roxo px-6 py-3 font-medium text-white transition hover:bg-ponti-roxo/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-marinho"
          >
            {{ textoCancelar() }}
          </button>
        </div>
      </div>
    </dialog>
  `,
})
export class DialogoConfirmacaoComponent {
  titulo = input('Sair sem salvar?');
  mensagem = input('Você tem alterações que ainda não foram salvas. Se sair agora, elas serão perdidas.');
  textoConfirmar = input('Sair sem salvar');
  textoCancelar = input('Continuar editando');

  private dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private resolver: ((confirmou: boolean) => void) | null = null;

  perguntar(): Promise<boolean> {
    // Se já estiver aberta, a pergunta anterior é considerada cancelada.
    this.resolver?.(false);
    return new Promise<boolean>((resolve) => {
      this.resolver = resolve;
      this.dialogo().nativeElement.showModal();
    });
  }

  responder(confirmou: boolean) {
    this.dialogo().nativeElement.close();
    this.resolver?.(confirmou);
    this.resolver = null;
  }

  // Esc: conta como "continuar editando" (a opção que não perde nada)
  aoCancelar(evento: Event) {
    evento.preventDefault();
    this.responder(false);
  }

  // Clique no fundo escuro (fora da janela): também continua editando
  aoClicarFora(evento: MouseEvent) {
    if (evento.target === this.dialogo().nativeElement) this.responder(false);
  }
}
