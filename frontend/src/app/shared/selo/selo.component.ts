import { Component, computed, input } from '@angular/core';

export type TomSelo = 'destaque' | 'aviso' | 'perigo' | 'sucesso' | 'neutro';

const CORES: Record<TomSelo, string> = {
  destaque: 'bg-ponti-lilas/40 text-ponti-marinho',
  aviso: 'bg-ponti-amarelo/30 text-ponti-marinho',
  perigo: 'bg-red-100 text-red-700',
  sucesso: 'bg-green-100 text-green-800',
  neutro: 'bg-gray-200 text-gray-700',
};

// Etiqueta arredondada de situação. Uso: <app-selo tom="sucesso">Em dia</app-selo>
@Component({
  selector: 'app-selo',
  standalone: true,
  host: { class: 'inline-flex items-center whitespace-nowrap rounded-full px-3 py-1 text-sm font-medium', '[class]': 'cores()' },
  template: `<ng-content />`,
})
export class SeloComponent {
  tom = input<TomSelo>('neutro');
  cores = computed(() => CORES[this.tom()]);
}
