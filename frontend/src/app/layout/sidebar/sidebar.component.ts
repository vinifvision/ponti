import { Component, computed, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { rotaInicialDe } from '../../auth/acesso';
import { SessaoService } from '../../auth/sessao.service';
import { IconeComponent, NomeIcone } from '../../shared/icone/icone.component';

interface ItemNavegacao {
  rotulo: string;
  rota: string;
  icone: NomeIcone;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, IconeComponent],
  templateUrl: './sidebar.component.html',
})
export class SidebarComponent {
  recolhida = input(false);
  alternar = output<void>();

  private sessao = inject(SessaoService);

  // Tela inicial de cada tipo de conta (o logo leva para ela)
  readonly inicio = computed(() => rotaInicialDe(this.sessao.tipo()));

  // Startup procura mentores e acompanha as sessões; mentor define a agenda, confirma pedidos e procura startups.
  readonly itens = computed<ItemNavegacao[]>(() =>
    this.sessao.tipo() === 'MENTOR'
      ? [
          { rotulo: 'Encontrar Startups', rota: '/encontrar-startups', icone: 'busca' },
          { rotulo: 'Mentorias', rota: '/mentorias', icone: 'agenda' },
          { rotulo: 'Minha agenda', rota: '/agenda', icone: 'agenda' },
          { rotulo: 'Perfil', rota: '/perfil', icone: 'usuario' },
        ]
      : [
          { rotulo: 'Encontrar Mentores', rota: '/encontrar-mentores', icone: 'busca' },
          { rotulo: 'Mentorias', rota: '/mentorias', icone: 'agenda' },
          { rotulo: 'Perfil', rota: '/perfil', icone: 'usuario' },
        ],
  );

}
