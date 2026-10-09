import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Mentor } from '../../models';
import { iniciais } from '../../shared/texto.utils';

@Component({
  selector: 'app-mentor-destaque',
  standalone: true,
  imports: [RouterLink],
  template: `
    <aside class="flex flex-col rounded-xl border border-gray-300 bg-white p-4 "  aria-label="Mentor selecionado">
      @if (mentor().fotoUrl) {
        <img
          [src]="mentor().fotoUrl"
          [alt]="'Foto de ' + mentor().nome"
          class="h-28 w-full rounded-md object-cover"
        />
      } @else {
        <div
          class="flex h-40 w-full flex-col items-center justify-center gap-1 rounded-md bg-gradient-to-br from-ponti-lilas to-ponti-roxo p-2 text-center text-white"
        >
          <span class="text-3xl font-semibold">{{ iniciais() }}</span>
          <span class="text-xs font-medium">Video Mentoria com {{ mentor().nome }}</span>
        </div>
      }

      <div class="mt-3 flex flex-col gap-2">
        <a
          routerLink="/agenda"
          [queryParams]="{ mentor: mentor().id }"
          class="rounded-lg border border-ponti-lilas py-2 text-center text-sm font-medium text-gray-700 transition-colors hover:bg-ponti-lilas/20"
        >
          Ver calendário do Mentor
        </a>
        <a
          routerLink="/perfil"
          [queryParams]="{ mentor: mentor().id }"
          class="rounded-lg border border-ponti-lilas py-2 text-center text-sm font-medium text-gray-700 transition-colors hover:bg-ponti-lilas/20"
        >
          Ver Perfil
        </a>
      </div>
    </aside>
  `,
})
export class MentorDestaqueComponent {
  mentor = input.required<Mentor>();
  iniciais = computed(() => iniciais(this.mentor().nome));
}