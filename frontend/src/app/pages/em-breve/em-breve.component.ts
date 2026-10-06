import { Component, input } from '@angular/core';

// Página temporária para as rotas da sidebar que ainda não foram construídas.
@Component({
  selector: 'app-em-breve',
  standalone: true,
  template: `
    <section class="pt-6">
      <h1 class="text-3xl font-medium sm:text-4xl">{{ titulo() }}</h1>
      <p class="mt-4 text-gray-500">Esta tela ainda está em construção.</p>
    </section>
  `,
})
export class EmBreveComponent {
  titulo = input('Em breve');
}
