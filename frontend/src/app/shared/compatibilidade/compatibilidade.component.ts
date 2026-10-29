import { Component, computed, input } from '@angular/core';

// Mostra a compatibilidade do match (0 a 100%) com uma barra e os motivos escritos.
// Só aparece quando o backend mandou o cálculo (listas "recomendados").
@Component({
  selector: 'app-compatibilidade',
  standalone: true,
  template: `
    @if (pontos() !== null && pontos() !== undefined) {
      <div class="mt-4 w-full max-w-md rounded-lg border p-3" [class]="caixa()">
        <p class="flex items-baseline justify-between gap-3 text-sm font-semibold">
          <span>{{ pontos() }}% compatível</span>
          <span class="text-xs font-medium opacity-80">{{ nivel() }}</span>
        </p>
        <div class="mt-2 h-2 w-full overflow-hidden rounded-full bg-white/70" role="img" [attr.aria-label]="pontos() + '% de compatibilidade'">
          <div class="h-full rounded-full transition-all" [class]="barra()" [style.width.%]="pontos()"></div>
        </div>
        @if (motivos()?.length) {
          <ul class="mt-2 list-disc space-y-0.5 pl-5 text-sm text-gray-700">
            @for (m of motivos(); track m) {
              <li>{{ m }}</li>
            }
          </ul>
        }
      </div>
    }
  `,
})
export class CompatibilidadeComponent {
  pontos = input<number | null | undefined>(null);
  motivos = input<string[] | null | undefined>([]);

  nivel = computed(() => {
    const p = this.pontos() ?? 0;
    return p >= 70 ? 'Alta' : p >= 40 ? 'Média' : 'Baixa';
  });

  caixa = computed(() => {
    const p = this.pontos() ?? 0;
    return p >= 70
      ? 'border-ponti-roxo bg-ponti-lilas/20 text-ponti-marinho'
      : p >= 40
        ? 'border-ponti-lilas bg-ponti-lilas/10 text-ponti-marinho'
        : 'border-gray-200 bg-gray-50 text-gray-700';
  });

  barra = computed(() => ((this.pontos() ?? 0) >= 40 ? 'bg-ponti-roxo' : 'bg-gray-400'));
}
