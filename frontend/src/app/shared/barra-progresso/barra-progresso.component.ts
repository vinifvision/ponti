import { Component, computed, input } from '@angular/core';

// Barra de progresso de 0 a 100. O rótulo é lido por leitores de tela.
@Component({
  selector: 'app-barra-progresso',
  standalone: true,
  host: { class: 'block' },
  template: `
    <div
      class="h-2 w-full overflow-hidden rounded-full bg-gray-100"
      role="progressbar"
      aria-valuemin="0"
      aria-valuemax="100"
      [attr.aria-valuenow]="limitado()"
      [attr.aria-label]="rotulo()"
    >
      <div class="h-full rounded-full bg-ponti-roxo transition-all" [style.width.%]="limitado()"></div>
    </div>
  `,
})
export class BarraProgressoComponent {
  valor = input.required<number>();
  rotulo = input('Progresso');
  limitado = computed(() => Math.min(100, Math.max(0, this.valor())));
}
