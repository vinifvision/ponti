import { Component, computed, input, output } from '@angular/core';
import { Mentor } from '../../models';
import { IconeComponent } from '../../shared/icone/icone.component';
import { iniciais } from '../../shared/texto.utils';

@Component({
  selector: 'app-mentor-card',
  standalone: true,
  imports: [IconeComponent],
  templateUrl: './mentor-card.component.html',
})
export class MentorCardComponent {
  mentor = input.required<Mentor>();
  compativel = input(false);
  selecionado = input(false);
  conectado = input(false);

  selecionar = output<Mentor>();
  conectar = output<Mentor>();

  iniciais = computed(() => iniciais(this.mentor().nome));
}