import { Component, input } from '@angular/core';

// Bolinha de aviso (mesmo visual da bolinha do sino no topo). Fica ao lado do rótulo de uma aba para dizer
// que ali dentro há algo esperando a pessoa. Uso: <app-bolinha-notificacao rotulo="Há atividades atrasadas" />
@Component({
  selector: 'app-bolinha-notificacao',
  standalone: true,
  host: { class: 'inline-block size-2.5 shrink-0 rounded-full bg-ponti-amarelo ring-2 ring-white' },
  template: `<span class="sr-only">{{ rotulo() }}</span>`,
})
export class BolinhaNotificacaoComponent {
  // Texto para leitores de tela (a bolinha sozinha não diz nada para quem não enxerga)
  rotulo = input('Tem novidades');
}
