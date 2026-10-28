import { Component, computed, input } from '@angular/core';

// Mostra a nota em estrelas (ex.: 4,7 -> ★★★★★ com a última parcialmente preenchida).
@Component({
  selector: 'app-estrelas',
  standalone: true,
  host: { class: 'inline-flex items-center', role: 'img', '[attr.aria-label]': 'rotulo()' },
  template: `
    @for (p of preenchimentos(); track $index) {
      <svg [attr.width]="tamanho()" [attr.height]="tamanho()" viewBox="0 0 24 24" aria-hidden="true">
        <defs>
          <linearGradient [attr.id]="idGrad + $index">
            <stop [attr.offset]="p + '%'" stop-color="#f5b301" />
            <stop [attr.offset]="p + '%'" stop-color="#d1d5db" />
          </linearGradient>
        </defs>
        <path
          [attr.fill]="'url(#' + idGrad + $index + ')'"
          d="M12 2.5l2.94 5.96 6.56.95-4.75 4.63 1.12 6.53L12 17.5l-5.87 3.07 1.12-6.53L2.5 9.41l6.56-.95z"
        />
      </svg>
    }
  `,
})
export class EstrelasComponent {
  private static contador = 0;
  readonly idGrad = `estrela-${EstrelasComponent.contador++}-`;

  nota = input.required<number>();
  tamanho = input(18);

  preenchimentos = computed(() =>
    [0, 1, 2, 3, 4].map((i) => Math.round(Math.max(0, Math.min(1, this.nota() - i)) * 100)),
  );
  rotulo = computed(() => `Nota ${this.nota().toLocaleString('pt-BR')} de 5`);
}
