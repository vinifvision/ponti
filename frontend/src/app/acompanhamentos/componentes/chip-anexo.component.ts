import { NgTemplateOutlet } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { IconeComponent } from '../../shared/icone/icone.component';

// Um anexo (arquivo) como "etiqueta": ícone, nome (cortado se for longo) e tamanho.
// Com "url" vira link para baixar (abre em outra aba); sem "url" é só texto (dados de exemplo ou arquivo ainda
// não enviado). Com "removivel" mostra o X para tirar da lista.
@Component({
  selector: 'app-chip-anexo',
  standalone: true,
  imports: [NgTemplateOutlet, IconeComponent],
  host: { class: 'inline-flex max-w-full' },
  template: `
    @if (url(); as href) {
      <a
        [href]="href"
        [attr.download]="nome()"
        target="_blank"
        rel="noopener noreferrer"
        class="inline-flex min-w-0 items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm font-medium text-ponti-marinho transition-colors hover:bg-gray-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-marinho"
        [class.rounded-r-none]="removivel()"
      >
        <ng-container *ngTemplateOutlet="conteudo" />
        <span class="sr-only">(abre em outra aba)</span>
      </a>
    } @else {
      <span
        class="inline-flex min-w-0 items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm font-medium text-ponti-marinho"
        [class.rounded-r-none]="removivel()"
      >
        <ng-container *ngTemplateOutlet="conteudo" />
      </span>
    }
    @if (removivel()) {
      <button
        type="button"
        (click)="remover.emit()"
        [attr.aria-label]="'Remover ' + nome()"
        class="cursor-pointer rounded-r-lg border border-l-0 border-gray-200 bg-gray-50 px-2 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-marinho"
      >
        <app-icone nome="fechar" [tamanho]="16" />
      </button>
    }

    <ng-template #conteudo>
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="size-4 shrink-0"
        aria-hidden="true"
      >
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <path d="M14 2v6h6" />
      </svg>
      <span class="truncate">{{ nome() }}</span>
      @if (detalhe()) {
        <span class="shrink-0 font-normal text-gray-500">{{ detalhe() }}</span>
      }
    </ng-template>
  `,
})
export class ChipAnexoComponent {
  nome = input.required<string>();
  // Texto pequeno ao lado do nome (o tamanho do arquivo)
  detalhe = input('');
  url = input<string | null>(null);
  removivel = input(false);

  remover = output<void>();
}
