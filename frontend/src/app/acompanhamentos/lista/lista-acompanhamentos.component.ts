import { Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { SessaoService } from '../../auth/sessao.service';
import { DialogoConfirmacaoComponent } from '../../shared/dialogo-confirmacao/dialogo-confirmacao.component';
import { AcompanhamentoService } from '../acompanhamento.service';
import { PainelConsolidado, ResumoAcompanhamento } from '../acompanhamento.models';
import { agruparPorStatus } from '../acompanhamento.utils';
import { CardAcompanhamentoComponent } from '../componentes/card-acompanhamento.component';
import { PainelProgressoComponent } from '../componentes/painel-progresso.component';

// Tela 1 do módulo: acompanhamentos separados em propostas pendentes, ativos e encerrados.
// A startup vê no topo o painel de progresso de todos os mentores; o mentor vê as startups dele.
@Component({
  selector: 'app-lista-acompanhamentos',
  standalone: true,
  imports: [RouterLink, CardAcompanhamentoComponent, PainelProgressoComponent, DialogoConfirmacaoComponent],
  templateUrl: './lista-acompanhamentos.component.html',
})
export class ListaAcompanhamentosComponent implements OnInit {
  private service = inject(AcompanhamentoService);
  private sessao = inject(SessaoService);
  private confirmarRecusa = viewChild.required(DialogoConfirmacaoComponent);

  ehMentor = computed(() => this.sessao.tipo() === 'MENTOR');

  lista = signal<ResumoAcompanhamento[]>([]);
  painel = signal<PainelConsolidado | null>(null);
  carregando = signal(true);
  erro = signal('');

  grupos = computed(() => agruparPorStatus(this.lista()));
  vazio = computed(() => {
    const g = this.grupos();
    return !g.pendentes.length && !g.ativos.length && !g.encerrados.length;
  });
  // O painel só faz sentido para a startup e quando já existe algo em andamento
  mostrarPainel = computed(() => !this.ehMentor() && (this.painel()?.acompanhamentosAtivos ?? 0) > 0);

  ngOnInit() {
    this.carregar(true);
  }

  private carregar(mostrarCarregando: boolean) {
    if (mostrarCarregando) this.carregando.set(true);
    this.erro.set('');
    forkJoin({
      lista: this.service.listar(),
      painel: this.ehMentor() ? of(null) : this.service.painel(),
    }).subscribe({
      next: ({ lista, painel }) => {
        this.lista.set(lista);
        this.painel.set(painel);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar os acompanhamentos. Tente de novo em instantes.');
        this.carregando.set(false);
      },
    });
  }

  aceitar(a: ResumoAcompanhamento) {
    this.service.aceitar(a.id).subscribe({
      next: () => this.carregar(false),
      error: () => this.erro.set('Não foi possível aceitar a proposta. Tente de novo.'),
    });
  }

  async recusar(a: ResumoAcompanhamento) {
    const confirmou = await this.confirmarRecusa().perguntar();
    if (!confirmou) return;
    this.service.recusar(a.id).subscribe({
      next: () => this.carregar(false),
      error: () => this.erro.set('Não foi possível recusar a proposta. Tente de novo.'),
    });
  }
}
