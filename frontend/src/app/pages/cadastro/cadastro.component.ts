import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthCabecalhoComponent } from '../../layout/auth-layout/auth-cabecalho.component';

// Primeiro passo do cadastro: a pessoa escolhe se é Startup ou Mentor.
@Component({
  selector: 'app-cadastro',
  standalone: true,
  imports: [RouterLink, AuthCabecalhoComponent],
  template: `
    <section class="flex w-full max-w-[30rem] flex-col items-center">
      <app-auth-cabecalho voltarPara="/bem-vindo" />

      <h1 class="mt-12 text-4xl font-semibold text-auth-marinho">Faça seu cadastro</h1>
      <p class="mt-2 text-center text-xl">Cadastre-se como uma Startup ou Mentor</p>

      <div class="mt-12 flex w-full flex-col gap-6">
        <a
          routerLink="/cadastro/startup"
          class="rounded-full border border-auth-lilas bg-white py-5 text-center text-xl text-auth-marinho transition-colors hover:bg-auth-lilas/15 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-auth-marinho"
        >
          Sou uma Startup
        </a>
        <a
          routerLink="/cadastro/mentor"
          class="rounded-full border border-auth-lilas bg-white py-5 text-center text-xl text-auth-marinho transition-colors hover:bg-auth-lilas/15 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-auth-marinho"
        >
          Sou um Mentor
        </a>
      </div>
    </section>
  `,
})
export class CadastroComponent {}
