import { Injectable, computed, signal } from '@angular/core';
import type { OpcaoFiltro } from '../shared/filtro-select/filtro-select.component';

// Guarda o que está aplicado na busca. A navbar grava aqui quando a pessoa aperta Enter
// e as telas "Encontrar mentores" / "Encontrar startups" leem daqui para filtrar a lista.
@Injectable({ providedIn: 'root' })
export class BuscaService {
  readonly termo = signal('');

  // Startup procurando mentores
  readonly area = signal('');
  readonly experiencia = signal('');
  readonly faixaPreco = signal('');
  readonly ordenacaoMentores = signal('');

  // Mentor procurando startups
  readonly segmento = signal('');
  readonly maturidade = signal('');
  readonly ordenacaoStartups = signal('');

  // Opções que dependem dos dados carregados (a tela de busca ou a navbar preenchem)
  readonly opcoesArea = signal<OpcaoFiltro[]>([]);
  readonly opcoesSegmento = signal<OpcaoFiltro[]>([]);

  // Tem busca valendo na lista? (texto ou algum filtro; a ordenação não conta como busca)
  readonly ativa = computed(
    () =>
      this.termo().trim() !== '' ||
      [this.area(), this.experiencia(), this.faixaPreco(), this.segmento(), this.maturidade()].some((v) => v !== ''),
  );

  // Cancela a busca: tira o texto e os filtros e a lista volta a mostrar tudo (a ordenação fica como está)
  limparBusca() {
    this.termo.set('');
    this.area.set('');
    this.experiencia.set('');
    this.faixaPreco.set('');
    this.segmento.set('');
    this.maturidade.set('');
  }

  // Volta tudo ao início (ao sair da conta, para a próxima pessoa não herdar a busca)
  limpar() {
    this.limparBusca();
    this.ordenacaoMentores.set('');
    this.ordenacaoStartups.set('');
    this.opcoesArea.set([]);
    this.opcoesSegmento.set([]);
  }
}
