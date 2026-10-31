import { Component, computed, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Mentor } from '../../models';
import { idVideoYoutube } from '../../shared/mentoria.utils';
import { iniciais } from '../../shared/texto.utils';

// Painel que aparece ao lado do card do mentor quando o mouse está em cima dele.
// A altura vem de quem o usa (h-full): o painel ocupa exatamente a altura do card do mentor.
// Na parte de cima mostra a miniatura (parada, sem tocar) do vídeo de apresentação do mentor;
// clicar nela abre o perfil, onde o vídeo toca. Sem vídeo, mostra a foto; sem foto, as iniciais.
@Component({
  selector: 'app-mentor-destaque',
  standalone: true,
  imports: [RouterLink],
  template: `
    <aside class="flex h-full flex-col rounded-xl border border-gray-300 bg-white p-4" aria-label="Mentor selecionado">
      <div class="relative min-h-28 flex-1 overflow-hidden rounded-md bg-gray-100">
        @if (idVideo(); as id) {
          <a
            [routerLink]="['/mentores', mentor().id, 'perfil']"
            class="group absolute inset-0 block focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-ponti-marinho"
            [attr.aria-label]="'Assistir ao vídeo de apresentação de ' + mentor().nome"
          >
            <img [src]="capa()" alt="" decoding="async" class="size-full object-cover" (error)="aoFalharCapa(id)" />
            <span
              class="absolute inset-0 flex items-center justify-center bg-black/10 transition-colors group-hover:bg-black/25"
              aria-hidden="true"
            >
              <span class="flex size-14 items-center justify-center rounded-full bg-white/90 text-ponti-roxo shadow-md">
                <svg viewBox="0 0 24 24" fill="currentColor" class="ml-1 size-7"><path d="M8 5v14l11-7z" /></svg>
              </span>
            </span>
          </a>
        } @else if (mentor().fotoUrl) {
          <img [src]="mentor().fotoUrl" [alt]="'Foto de ' + mentor().nome" class="absolute inset-0 size-full object-cover" />
        } @else {
          <div
            class="absolute inset-0 flex flex-col items-center justify-center gap-1 bg-gradient-to-br from-ponti-lilas to-ponti-roxo p-2 text-center text-white"
          >
            <span class="text-3xl font-semibold">{{ iniciais() }}</span>
            <span class="text-xs font-medium">Video Mentoria com {{ mentor().nome }}</span>
          </div>
        }
      </div>

      <div class="mt-3 flex shrink-0 flex-col gap-2">
        <a
          [routerLink]="['/mentores', mentor().id, 'agendar']"
          class="rounded-lg border border-ponti-lilas py-2 text-center text-sm font-medium text-gray-700 transition-colors hover:bg-ponti-lilas/20"
        >
          Ver calendário do Mentor
        </a>
        <a
          [routerLink]="['/mentores', mentor().id, 'perfil']"
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

  // Só o id de 11 caracteres do YouTube entra no endereço da imagem (link estranho não vira <img>)
  idVideo = computed(() => idVideoYoutube(this.mentor().videoUrl));

  // Id do vídeo cuja miniatura em alta resolução não existe (nem todo vídeo tem): cai para a de 320x180
  private semMiniaturaHd = signal<string | null>(null);
  capa = computed(() => {
    const id = this.idVideo();
    if (!id) return '';
    return `https://i.ytimg.com/vi/${id}/${this.semMiniaturaHd() === id ? 'mqdefault' : 'maxresdefault'}.jpg`;
  });

  aoFalharCapa(id: string) {
    this.semMiniaturaHd.set(id);
  }
}
