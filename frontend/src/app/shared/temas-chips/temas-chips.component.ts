import { Component, OnInit, inject, input } from '@angular/core';
import { TemasService } from '../temas.service';

// Mostra os temas de um perfil como etiquetas (só leitura).
@Component({
  selector: 'app-temas-chips',
  standalone: true,
  template: `
    @if (codigos()?.length) {
      <ul class="flex flex-wrap gap-2" [attr.aria-label]="rotulo()">
        @for (c of codigos(); track c) {
          <li class="rounded-full bg-ponti-lilas/30 px-3 py-1 text-sm font-medium text-ponti-marinho">{{ temas.nome(c) }}</li>
        }
      </ul>
    }
  `,
})
export class TemasChipsComponent implements OnInit {
  readonly temas = inject(TemasService);

  codigos = input<string[] | null | undefined>([]);
  rotulo = input('Temas');

  ngOnInit() {
    this.temas.carregar();
  }
}
