import { Component, computed, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Startup } from '../../models';
import { idVideoYoutube } from '../../shared/mentoria.utils';
import { iniciais } from '../../shared/texto.utils';

// Painel que aparece ao lado do card da startup quando o mouse está em cima dele (tela do mentor).
// A altura vem de quem o usa (h-full): o painel ocupa exatamente a altura do card da startup.
// Na parte de cima mostra a miniatura (parada, sem tocar) do vídeo de pitch da startup; clicar nela abre
// o perfil, onde o vídeo toca. Sem vídeo, mostra o logo; sem logo, as iniciais.
@Component({
  selector: 'app-startup-destaque',
  standalone: true,
  imports: [RouterLink],
  template: `
    <aside class="flex h-full flex-col rounded-xl border border-gray-300 bg-white p-4" aria-label="Startup selecionada">
      <div class="relative min-h-28 flex-1 overflow-hidden rounded-md bg-gray-100">
        @if (idVideo(); as id) {
          <a
            [routerLink]="['/startups', startup().id, 'perfil']"
            [state]="estado()"
            class="group absolute inset-0 block focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-ponti-marinho"
            [attr.aria-label]="'Assistir ao pitch de ' + startup().nome"
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
        } @else if (startup().fotoUrl) {
          <img [src]="startup().fotoUrl" [alt]="'Logo de ' + startup().nome" class="absolute inset-0 size-full object-cover" />
        } @else {
          <div
            class="absolute inset-0 flex flex-col items-center justify-center gap-1 bg-gradient-to-br from-ponti-lilas to-ponti-roxo p-2 text-center text-white"
          >
            <span class="text-3xl font-semibold">{{ iniciais() }}</span>
            <span class="text-xs font-medium">Pitch de {{ startup().nome }}</span>
          </div>
        }
      </div>

      <div class="mt-3 flex shrink-0 flex-col gap-2">
        <a
          [routerLink]="['/startups', startup().id, 'perfil']"
          [state]="estado()"
          class="rounded-lg border border-ponti-lilas py-2 text-center text-sm font-medium text-gray-700 transition-colors hover:bg-ponti-lilas/20"
        >
          Ver perfil
        </a>
        <button
          type="button"
          (click)="convidar.emit(startup())"
          [disabled]="!!situacao()"
          class="cursor-pointer rounded-lg bg-ponti-roxo py-2 text-center text-sm font-medium text-white transition-colors hover:bg-ponti-roxo/90 disabled:cursor-default disabled:bg-gray-200 disabled:text-gray-600"
        >
          {{ situacao() ?? 'Sugerir mentoria' }}
        </button>
      </div>
    </aside>
  `,
})
export class StartupDestaqueComponent {
  startup = input.required<Startup>();
  // Texto do botão quando já existe sugestão entre os dois (igual ao do card)
  situacao = input<string | null>(null);

  convidar = output<Startup>();

  iniciais = computed(() => iniciais(this.startup().nome));

  // O perfil mostra a compatibilidade que já veio na lista, sem pedir de novo
  estado = computed(() => ({ compatibilidade: this.startup().compatibilidade, motivos: this.startup().motivos }));

  // Só o id de 11 caracteres do YouTube entra no endereço da imagem (link estranho não vira <img>)
  idVideo = computed(() => idVideoYoutube(this.startup().videoPitchUrl));

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
