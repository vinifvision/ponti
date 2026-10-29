import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Startup } from '../../models';
import { CompatibilidadeComponent } from '../../shared/compatibilidade/compatibilidade.component';
import { TemasChipsComponent } from '../../shared/temas-chips/temas-chips.component';
import { IconeComponent } from '../../shared/icone/icone.component';
import { NIVEL_MAXIMO, nomeDoNivel } from '../../shared/maturidade.utils';
import { iniciais } from '../../shared/texto.utils';

// Card de startup na tela "Encontrar startups" do mentor (mesmo estilo do card de mentor)
@Component({
  selector: 'app-startup-card',
  standalone: true,
  imports: [IconeComponent, RouterLink, CompatibilidadeComponent, TemasChipsComponent],
  templateUrl: './startup-card.component.html',
})
export class StartupCardComponent {
  startup = input.required<Startup>();
  // Texto do botão quando já existe convite entre os dois (enviado ou recebido)
  situacao = input<string | null>(null);

  convidar = output<Startup>();

  readonly nivelMaximo = NIVEL_MAXIMO;
  ehPet = computed(() => /pet/i.test(this.startup().segmento ?? ''));
  iniciais = computed(() => iniciais(this.startup().nome));
  nivel = computed(() => this.startup().estagioMaturidade ?? null);
  nomeNivel = computed(() => nomeDoNivel(this.nivel()));
  local = computed(() => [this.startup().cidade, this.startup().estado].filter(Boolean).join(' - '));
}
