import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BarraProgressoComponent } from '../../shared/barra-progresso/barra-progresso.component';
import { SeloComponent } from '../../shared/selo/selo.component';
import { iniciais } from '../../shared/texto.utils';
import { ResumoAcompanhamento } from '../acompanhamento.models';
import { dataCurta, percentual, plural, selosDoCard } from '../acompanhamento.utils';

// Card de um acompanhamento na lista. O visual muda conforme o status: proposta (destaque lilás),
// ativo (progresso e atalho para abrir) e encerrado (apagado, somente leitura).
@Component({
  selector: 'app-card-acompanhamento',
  standalone: true,
  imports: [RouterLink, SeloComponent, BarraProgressoComponent],
  templateUrl: './card-acompanhamento.component.html',
})
export class CardAcompanhamentoComponent {
  acompanhamento = input.required<ResumoAcompanhamento>();
  ehMentor = input(false);

  aceitar = output<ResumoAcompanhamento>();
  recusar = output<ResumoAcompanhamento>();

  readonly dataCurta = dataCurta;

  selos = computed(() => selosDoCard(this.acompanhamento(), this.ehMentor()));
  iniciais = computed(() => iniciais(this.acompanhamento().parceiro.nome));
  progressoAtividades = computed(() =>
    percentual(this.acompanhamento().atividadesAprovadas, this.acompanhamento().atividadesTotal),
  );
  metasSugeridas = computed(() => plural(this.acompanhamento().metasSugeridas ?? 0, 'meta sugerida', 'metas sugeridas'));

  // O perfil que abre é o do parceiro: mentor (visão da startup) ou startup (visão do mentor)
  rotaPerfil = computed(() => [this.ehMentor() ? '/startups' : '/mentores', this.acompanhamento().parceiro.id, 'perfil']);
  textoPerfil = computed(() => (this.ehMentor() ? 'Ver perfil da startup' : 'Ver perfil do mentor'));

  classeCard = computed(() => {
    const status = this.acompanhamento().status;
    if (status === 'PROPOSTO' && !this.ehMentor()) return 'border-ponti-lilas bg-ponti-lilas/10';
    if (status === 'ENCERRADO') return 'border-gray-200 bg-gray-50';
    return 'border-gray-200 bg-white';
  });
}
