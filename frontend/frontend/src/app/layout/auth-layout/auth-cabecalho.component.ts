import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconeComponent } from '../../shared/icone/icone.component';

// Topo comum das telas públicas: logo lilás e, opcionalmente, a seta de voltar
// (posicionada no canto superior esquerdo do <main> do AuthLayout).
@Component({
  selector: 'app-auth-cabecalho',
  standalone: true,
  imports: [RouterLink, IconeComponent],
  host: { class: 'block' },
  template: `
    @if (voltarPara()) {
      <a
        [routerLink]="voltarPara()"
        class="absolute left-6 top-6 rounded-full p-2 text-auth-marinho transition-colors hover:bg-auth-lilas/20 focus-visible:outline-2 focus-visible:outline-auth-marinho sm:left-8 sm:top-8"
        aria-label="Voltar"
      >
        <app-icone nome="seta-esquerda" [tamanho]="36" />
      </a>
    }
    <img src="logo-lilas.svg" alt="Ponti" class="mx-auto w-44" />
  `,
})
export class AuthCabecalhoComponent {
  voltarPara = input<string>();
}
