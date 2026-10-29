import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Mentor } from '../../models';
import { IconeComponent } from '../../shared/icone/icone.component';
import { TemasChipsComponent } from '../../shared/temas-chips/temas-chips.component';
import { EstrelasComponent } from '../../shared/estrelas/estrelas.component';
import { formatarDuracao, formatarValorSessao } from '../../shared/mentoria.utils';
import { iniciais } from '../../shared/texto.utils';

@Component({
  selector: 'app-mentor-card',
  standalone: true,
  imports: [IconeComponent, RouterLink, EstrelasComponent, TemasChipsComponent],
  templateUrl: './mentor-card.component.html',
})
export class MentorCardComponent {
  mentor = input.required<Mentor>();
  compativel = input(false);
  selecionado = input(false);

  selecionar = output<Mentor>();

  iniciais = computed(() => iniciais(this.mentor().nome));
  valor = computed(() => formatarValorSessao(this.mentor().valorSessao));
  duracao = computed(() => formatarDuracao(this.mentor().duracaoSessaoMinutos));
}