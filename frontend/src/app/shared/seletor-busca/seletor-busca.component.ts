import { Component, computed, input, model, signal } from '@angular/core';

const normalizar = (t: string) =>
  t
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();

// Campo de digitar com sugestões. Com max=1 escolher outra opção substitui a atual.
// <app-seletor-busca rotulo="Setores" placeholder="Digite para buscar" [opcoes]="SETORES" [max]="3" [(selecionados)]="lista" />
@Component({
  selector: 'app-seletor-busca',
  standalone: true,
  template: `
    <div class="relative w-full">
      <label [for]="idCampo" class="sr-only">{{ rotulo() }}</label>
      <input
        [id]="idCampo"
        type="text"
        role="combobox"
        autocomplete="off"
        [placeholder]="placeholder()"
        [disabled]="desabilitado()"
        [attr.aria-expanded]="aberto() && sugestoes().length > 0"
        [attr.aria-controls]="idLista"
        [attr.aria-activedescendant]="aberto() && sugestoes().length ? idLista + '-' + ativo() : null"
        aria-autocomplete="list"
        [value]="busca()"
        (input)="digitar($any($event.target).value)"
        (focus)="aberto.set(true)"
        (blur)="fechar()"
        (keydown)="teclar($event)"
        class="w-full rounded-full border border-neutral-200 bg-neutral-200 px-8 py-4 text-lg text-gray-900 placeholder:text-neutral-400 focus:border-auth-lilas focus:outline-none focus:ring-2 focus:ring-auth-lilas/50 disabled:cursor-default"
      />

      @if (aberto() && !desabilitado()) {
        <ul
          [id]="idLista"
          role="listbox"
          [attr.aria-label]="rotulo()"
          class="absolute inset-x-0 z-20 mt-2 max-h-60 overflow-auto rounded-2xl border border-neutral-200 bg-white py-2 shadow-lg"
        >
          @for (o of sugestoes(); track o; let i = $index) {
            <li
              [id]="idLista + '-' + i"
              role="option"
              [attr.aria-selected]="marcado(o)"
              (mousedown)="$event.preventDefault(); escolher(o)"
              class="cursor-pointer px-6 py-2 text-base text-gray-900"
              [class]="i === ativo() ? 'bg-auth-lilas/30' : ''"
            >
              {{ o }}
              @if (marcado(o)) {
                <span class="ml-2 text-sm text-gray-500">(selecionado)</span>
              }
            </li>
          } @empty {
            <li class="px-6 py-2 text-base text-gray-500" role="option" aria-disabled="true">
              Nenhuma opção encontrada
            </li>
          }
        </ul>
      }

      @if (max() > 1) {
        <p class="mt-2 px-2 text-sm text-gray-600">Escolha até {{ max() }} ({{ selecionados().length }}/{{ max() }})</p>
      }

      @if (selecionados().length > 0) {
        <ul class="mt-3 flex flex-wrap gap-2" aria-label="Selecionados">
          @for (s of selecionados(); track s) {
            <li class="inline-flex items-center gap-2 rounded-full bg-auth-lilas/40 py-1.5 pl-4 pr-2 text-sm font-medium text-gray-900">
              {{ s }}
              <button
                type="button"
                [disabled]="desabilitado()"
                (click)="remover(s)"
                [attr.aria-label]="'Remover ' + s"
                class="flex size-6 cursor-pointer items-center justify-center rounded-full text-base leading-none hover:bg-white/70 disabled:cursor-default"
              >
                &times;
              </button>
            </li>
          }
        </ul>
      }
    </div>
  `,
})
export class SeletorBuscaComponent {
  private static proximoId = 0;
  readonly idCampo = `busca-${SeletorBuscaComponent.proximoId}`;
  readonly idLista = `${this.idCampo}-lista`;
  constructor() {
    SeletorBuscaComponent.proximoId++;
  }

  rotulo = input('Opções');
  placeholder = input('Digite para buscar');
  opcoes = input<readonly string[]>([]);
  max = input(1);
  desabilitado = input(false);
  selecionados = model<string[]>([]);

  busca = signal('');
  aberto = signal(false);
  ativo = signal(0);

  sugestoes = computed(() => {
    const termo = normalizar(this.busca().trim());
    const lista = this.opcoes();
    return termo ? lista.filter((o) => normalizar(o).includes(termo)) : [...lista];
  });

  marcado(o: string): boolean {
    return this.selecionados().includes(o);
  }

  digitar(valor: string) {
    this.busca.set(valor);
    this.ativo.set(0);
    this.aberto.set(true);
  }

  escolher(o: string) {
    if (this.marcado(o)) {
      this.remover(o);
    } else if (this.max() === 1) {
      this.selecionados.set([o]);
    } else if (this.selecionados().length < this.max()) {
      this.selecionados.update((l) => [...l, o]);
    }
    this.busca.set('');
    this.ativo.set(0);
    if (this.max() === 1 || this.selecionados().length >= this.max()) this.aberto.set(false);
  }

  remover(o: string) {
    this.selecionados.update((l) => l.filter((x) => x !== o));
  }

  fechar() {
    this.aberto.set(false);
    this.busca.set('');
  }

  teclar(e: KeyboardEvent) {
    const lista = this.sugestoes();
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      this.aberto.set(true);
      this.ativo.set(Math.min(this.ativo() + 1, Math.max(lista.length - 1, 0)));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      this.ativo.set(Math.max(this.ativo() - 1, 0));
    } else if (e.key === 'Enter') {
      if (this.aberto() && lista[this.ativo()]) {
        e.preventDefault();
        this.escolher(lista[this.ativo()]);
      }
    } else if (e.key === 'Escape') {
      this.aberto.set(false);
    } else if (e.key === 'Backspace' && !this.busca() && this.selecionados().length > 0) {
      this.remover(this.selecionados()[this.selecionados().length - 1]);
    }
  }
}
