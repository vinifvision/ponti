import { Component, OnInit, inject, input, model } from '@angular/core';
import { TemasService } from '../temas.service';

// Escolha de temas em "chips": clica para marcar e clica de novo para desmarcar, até o limite.
// Uso: <app-seletor-temas rotulo="Em que você precisa de ajuda? *" [max]="3" [(selecionados)]="temas" />
@Component({
  selector: 'app-seletor-temas',
  standalone: true,
  template: `
    <fieldset class="w-full">
      <legend class="text-base font-semibold text-gray-800">{{ rotulo() }}</legend>
      <p class="mt-1 text-sm text-gray-600">Escolha até {{ max() }} ({{ selecionados().length }}/{{ max() }})</p>
      <div class="mt-3 flex flex-wrap gap-2">
        @for (t of temas.lista(); track t.codigo) {
          <button
            type="button"
            [attr.aria-pressed]="marcado(t.codigo)"
            [disabled]="desabilitado() || (!marcado(t.codigo) && cheio())"
            (click)="alternar(t.codigo)"
            class="cursor-pointer rounded-full border px-4 py-2 text-sm font-medium transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-marinho disabled:cursor-default disabled:opacity-40"
            [class]="marcado(t.codigo) ? 'border-ponti-roxo bg-ponti-roxo text-white' : 'border-gray-300 bg-white text-gray-800 hover:border-ponti-roxo'"
          >
            {{ t.nome }}
          </button>
        }
      </div>
      @if (temas.falhou()) {
        <p class="mt-2 text-sm text-red-600" role="alert">
          Não foi possível carregar os temas.
          <button type="button" class="cursor-pointer font-semibold underline" (click)="temas.carregar()">Tentar de novo</button>
        </p>
      }
    </fieldset>
  `,
})
export class SeletorTemasComponent implements OnInit {
  readonly temas = inject(TemasService);

  rotulo = input('Temas');
  max = input(3);
  desabilitado = input(false);
  selecionados = model<string[]>([]);

  ngOnInit() {
    this.temas.carregar();
  }

  marcado(codigo: string): boolean {
    return this.selecionados().includes(codigo);
  }

  cheio(): boolean {
    return this.selecionados().length >= this.max();
  }

  alternar(codigo: string) {
    if (this.marcado(codigo)) {
      this.selecionados.update((lista) => lista.filter((c) => c !== codigo));
    } else if (!this.cheio()) {
      this.selecionados.update((lista) => [...lista, codigo]);
    }
  }
}
