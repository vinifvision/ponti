import { Component, computed, input } from '@angular/core';
import { BarraProgressoComponent } from '../../shared/barra-progresso/barra-progresso.component';
import { CartaoMetricaComponent } from '../../shared/cartao-metrica/cartao-metrica.component';
import { PainelConsolidado } from '../acompanhamento.models';
import { percentual } from '../acompanhamento.utils';

// Painel da startup no topo da lista: soma dos acompanhamentos ativos de todos os mentores.
@Component({
  selector: 'app-painel-progresso',
  standalone: true,
  imports: [CartaoMetricaComponent, BarraProgressoComponent],
  template: `
    <section aria-labelledby="titulo-painel-progresso">
      <h2 id="titulo-painel-progresso" class="text-2xl font-medium">
        Seu progresso geral
        <span class="ml-2 text-base font-normal text-gray-500">somando todos os mentores</span>
      </h2>

      <div class="mt-4 grid grid-cols-2 gap-3 md:grid-cols-4">
        <app-cartao-metrica rotulo="Acompanhamentos ativos" [valor]="'' + painel().acompanhamentosAtivos" />
        <app-cartao-metrica rotulo="Atividades aprovadas" [valor]="painel().atividadesAprovadas + ' de ' + painel().atividadesTotal">
          <app-barra-progresso class="mt-3" [valor]="progressoAtividades()" rotulo="Atividades aprovadas" />
        </app-cartao-metrica>
        <app-cartao-metrica rotulo="Entregas no prazo" [valor]="entregasNoPrazo()" />
        <app-cartao-metrica rotulo="Metas cumpridas" [valor]="painel().metasCumpridas + ' de ' + painel().metasTotal" />
      </div>
    </section>
  `,
})
export class PainelProgressoComponent {
  painel = input.required<PainelConsolidado>();

  progressoAtividades = computed(() => percentual(this.painel().atividadesAprovadas, this.painel().atividadesTotal));
  entregasNoPrazo = computed(() => {
    const valor = this.painel().entregasNoPrazo;
    return valor === null ? '-' : `${valor}%`;
  });
}
