import { Component, input, model } from '@angular/core';
import { IconeComponent } from '../icone/icone.component';

let proximoId = 0;

export interface OpcaoFiltro {
  valor: string;
  rotulo: string;
}

@Component({
  selector: 'app-filtro-select',
  standalone: true,
  imports: [IconeComponent],
  host: { class: 'block w-full sm:w-64' },
  template: `
    <div
      class="relative rounded-lg border border-ponti-lilas bg-white px-4 py-2 transition focus-within:border-ponti-roxo focus-within:ring-2 focus-within:ring-ponti-roxo/30"
    >
      <label [for]="id" class="block text-xs font-normal text-gray-500">{{ rotulo() }}</label>
      <select
        [id]="id"
        (change)="valor.set($any($event.target).value)"
        class="w-full cursor-pointer appearance-none border-0 bg-transparent p-0 pr-8 text-base font-medium text-gray-800 focus:outline-none"
      >
        <option value="" [selected]="!valor()">{{ placeholder() }}</option>
        @for (opcao of opcoes(); track opcao.valor) {
          <option [value]="opcao.valor" [selected]="opcao.valor === valor()">{{ opcao.rotulo }}</option>
        }
      </select>

      @if (limpavel() && valor()) {
        <button
          type="button"
          (click)="valor.set('')"
          class="absolute right-3 top-1/2 -translate-y-1/2 rounded p-1 text-gray-700 hover:bg-gray-100"
          [attr.aria-label]="'Limpar ' + rotulo()"
        >
          <app-icone nome="fechar" [tamanho]="20" />
        </button>
      } @else {
        <app-icone
          nome="seta-baixo"
          [tamanho]="22"
          class="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-gray-700"
        />
      }
    </div>
  `,
})
export class FiltroSelectComponent {
  rotulo = input.required<string>();
  opcoes = input<OpcaoFiltro[]>([]);
  placeholder = input('Selecionar');
  limpavel = input(false);
  valor = model('');

  readonly id = `filtro-${proximoId++}`;
}
