import { Component, input } from '@angular/core';

export type NomeIcone =
  | 'busca'
  | 'agenda'
  | 'usuario'
  | 'config'
  | 'recolher'
  | 'sino'
  | 'seta-baixo'
  | 'fechar'
  | 'maleta'
  | 'alvo'
  | 'brilho';

@Component({
  selector: 'app-icone',
  standalone: true,
  host: { class: 'inline-flex shrink-0', 'aria-hidden': 'true' },
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      [attr.width]="tamanho()"
      [attr.height]="tamanho()"
    >
      @switch (nome()) {
        @case ('busca') {
          <svg:circle cx="11" cy="11" r="8" />
          <svg:path d="m21 21-4.3-4.3" />
        }
        @case ('agenda') {
          <svg:rect width="18" height="18" x="3" y="4" rx="2" />
          <svg:path d="M8 2v4M16 2v4M3 10h18M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01M16 18h.01" />
        }
        @case ('usuario') {
          <svg:circle cx="12" cy="8" r="5" />
          <svg:path d="M20 21a8 8 0 0 0-16 0" />
        }
        @case ('config') {
          <svg:path
            d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"
          />
          <svg:circle cx="12" cy="12" r="3" />
        }
        @case ('recolher') {
          <svg:path d="M3 12h18M9 6h12M9 18h12M6 9l-3 3 3 3" />
        }
        @case ('sino') {
          <svg:path d="M10.268 21a2 2 0 0 0 3.464 0" />
          <svg:path
            d="M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"
          />
        }
        @case ('seta-baixo') {
          <svg:path d="m6 9 6 6 6-6" />
        }
        @case ('fechar') {
          <svg:path d="M18 6 6 18M6 6l12 12" />
        }
        @case ('maleta') {
          <svg:path d="M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
          <svg:rect width="20" height="14" x="2" y="6" rx="2" />
        }
        @case ('alvo') {
          <svg:circle cx="12" cy="12" r="10" />
          <svg:circle cx="12" cy="12" r="6" />
          <svg:circle cx="12" cy="12" r="2" />
        }
        @case ('brilho') {
          <svg:path
            d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"
          />
          <svg:path d="M20 3v4M22 5h-4" />
        }
      }
    </svg>
  `,
})
export class IconeComponent {
  nome = input.required<NomeIcone>();
  tamanho = input(24);
}
