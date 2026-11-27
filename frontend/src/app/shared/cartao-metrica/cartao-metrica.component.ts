import { Component, input } from '@angular/core';

// Cartão com um número em destaque. O conteúdo extra (ex.: uma barra de progresso) vai dentro da tag.
@Component({
  selector: 'app-cartao-metrica',
  standalone: true,
  host: { class: 'block rounded-xl border border-gray-200 bg-white p-4' },
  template: `
    <p class="text-sm text-gray-500">{{ rotulo() }}</p>
    <p class="mt-1 text-2xl font-semibold">{{ valor() }}</p>
    <ng-content />
  `,
})
export class CartaoMetricaComponent {
  rotulo = input.required<string>();
  valor = input.required<string>();
}
