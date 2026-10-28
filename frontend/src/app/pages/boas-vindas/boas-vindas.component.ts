import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthCabecalhoComponent } from '../../layout/auth-layout/auth-cabecalho.component';

@Component({
  selector: 'app-boas-vindas',
  standalone: true,
  imports: [RouterLink, AuthCabecalhoComponent],
  template: `
    <section class="flex w-full max-w-2xl flex-col">
      <app-auth-cabecalho />

      <h1 class="mt-28 text-4xl font-medium text-auth-marinho">Bem-vindos(a)!</h1>
      <p class="mt-5 text-justify text-lg leading-relaxed">
        Turbine sua startup com a experiência de quem já percorreu o caminho. A Ponti conecta startups e mentores
        pelo perfil, pelo estágio de maturidade e pelo ecossistema, para que boas ideias encontrem a mentoria
        certa para escalar.
      </p>

      <div class="mx-auto mt-24 flex w-full max-w-xs flex-col gap-4">
        <a
          routerLink="/login"
          class="rounded-full border border-neutral-300 bg-white py-5 text-center text-xl font-medium text-auth-marinho transition-colors hover:bg-neutral-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-auth-marinho"
        >
          Fazer login
        </a>
        <a
          routerLink="/cadastro"
          class="rounded-full border border-auth-lilas bg-auth-lilas py-5 text-center text-xl font-medium text-white transition hover:brightness-95 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-auth-marinho"
        >
          Cadastre-se
        </a>
      </div>
    </section>
  `,
})
export class BoasVindasComponent {}
