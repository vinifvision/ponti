import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
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

  readonly itens: ItemNavegacao[] = [
    { rotulo: 'Encontrar Mentores', rota: '/encontrar-mentores', icone: 'busca' },
    { rotulo: 'Agenda', rota: '/agenda', icone: 'agenda' },
    { rotulo: 'Perfil', rota: '/perfil', icone: 'usuario' },
  ];
}
