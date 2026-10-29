import { Component, computed, input } from '@angular/core';

// Mostra a compatibilidade do match (0 a 100%) com uma barra e os motivos escritos.
// Só aparece quando o backend mandou o cálculo (listas "recomendados").
@Component({
  selector: 'app-compatibilidade',
  standalone: true,
  template: `
    @if (pontos() !== null && pontos() !== undefined) {
      @if (compacto()) {
        <p
          class="w-full rounded-md bg-ponti-lilas/25 px-4 py-3 text-center text-lg font-semibold text-ponti-marinho"
          [attr.title]="motivos()?.join(' • ') || null"
        >
          {{ pontos() }}% compatível
        </p>
      } @else {
      <div class="mt-4 w-full max-w-2xl rounded-xl border-2 p-6" [class]="caixa()">
        <p class="flex items-baseline justify-between gap-3 text-xl font-semibold">
          <span>{{ pontos() }}% compatível</span>
          <span class="text-base font-medium opacity-80">{{ nivel() }}</span>
        </p>
        <div class="mt-3 h-3 w-full overflow-hidden rounded-full bg-white/70" role="img" [attr.aria-label]="pontos() + '% de compatibilidade'">
          <div class="h-full rounded-full transition-all" [class]="barra()" [style.width.%]="pontos()"></div>
        </div>
        @if (motivos()?.length) {
          <ul class="mt-4 list-disc space-y-1.5 pl-6 text-lg text-gray-700">
            @for (m of motivos(); track m) {
              <li>{{ m }}</li>
            }
          </ul>
        }
      </div>
      }
    }
  `,
})
export class CompatibilidadeComponent {
  pontos = input<number | null | undefined>(null);
  motivos = input<string[] | null | undefined>([]);
  compacto = input(false);

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
