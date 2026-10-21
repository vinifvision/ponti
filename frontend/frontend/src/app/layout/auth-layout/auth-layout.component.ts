import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

// Layout das telas públicas (boas-vindas, login e cadastro):
// painel lilás com a marca à esquerda e o conteúdo da tela à direita.
@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="flex min-h-screen bg-white font-sans text-gray-900">
      <aside
        class="hidden w-1/2 shrink-0 flex-col items-center justify-center rounded-r-[3rem] bg-auth-lilas text-auth-marinho lg:flex"
        aria-hidden="true"
      >
        <img src="logo.svg" alt="" class="w-3/5" />
        <p class="mt-3 whitespace-nowrap text-[2.3vw] font-light">Conectando a Inovação</p>
      </aside>

      <main class="relative flex min-w-0 flex-1 flex-col items-center px-6 pb-12 pt-[18vh] sm:px-12">
        <router-outlet />
      </main>
    </div>
  `,
})
export class AuthLayoutComponent {}
