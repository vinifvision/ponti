import { Component, OnInit, computed, inject, input, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { SessaoService } from '../../auth/sessao.service';
import { AbasListaComponent } from '../../shared/abas-lista/abas-lista.component';
import { AbaLista, escolherAba } from '../../shared/abas-lista/abas-lista.utils';
import { DialogoConfirmacaoComponent } from '../../shared/dialogo-confirmacao/dialogo-confirmacao.component';
import { AcompanhamentoService } from '../acompanhamento.service';
import { PainelConsolidado, ResumoAcompanhamento } from '../acompanhamento.models';
import { ABAS_LISTA_ACOMPANHAMENTOS, agruparPorStatus, notificacoesDaLista } from '../acompanhamento.utils';
import { CardAcompanhamentoComponent } from '../componentes/card-acompanhamento.component';
import { ModalNovoAcompanhamentoComponent } from '../componentes/modal-novo-acompanhamento.component';
import { PainelProgressoComponent } from '../componentes/painel-progresso.component';
import { IconeComponent } from '../../shared/icone/icone.component';

// Tela 1 do módulo: acompanhamentos separados, em abas, em propostas pendentes, ativos e encerrados.
// A aba aberta fica na URL (?aba=ativos); a bolinha de aviso marca a aba com algo esperando a pessoa.
// A startup vê no topo o painel de progresso de todos os mentores; o mentor vê as startups dele.
// O mentor também tem aqui o botão "Novo acompanhamento", no topo da tela (como o "Criar turma" do Google
// Classroom): escolhe uma startup elegível e abre a proposta.
@Component({
  selector: 'app-lista-acompanhamentos',
  standalone: true,
  imports: [
    RouterLink,
    CardAcompanhamentoComponent,
    PainelProgressoComponent,
    DialogoConfirmacaoComponent,
    ModalNovoAcompanhamentoComponent,
    IconeComponent,
    AbasListaComponent,
  ],
  templateUrl: './lista-acompanhamentos.component.html',
})
export class ListaAcompanhamentosComponent implements OnInit {
  private service = inject(AcompanhamentoService);
  private sessao = inject(SessaoService);
  private confirmarRecusa = viewChild.required(DialogoConfirmacaoComponent);
  // Só existe na tela do mentor
  private novoAcompanhamento = viewChild(ModalNovoAcompanhamentoComponent);

  ehMentor = computed(() => this.sessao.tipo() === 'MENTOR');

  // Vem da URL (?aba=...) por withComponentInputBinding
  aba = input<string>();

  lista = signal<ResumoAcompanhamento[]>([]);
  painel = signal<PainelConsolidado | null>(null);
  carregando = signal(true);
  erro = signal('');

  grupos = computed(() => agruparPorStatus(this.lista()));
  vazio = computed(() => {
    const g = this.grupos();
    return !g.pendentes.length && !g.ativos.length && !g.encerrados.length;
  });
  abaAtual = computed(() =>
    escolherAba(this.aba(), [
      { chave: 'pendentes' as const, quantidade: this.grupos().pendentes.length },
      { chave: 'ativos' as const, quantidade: this.grupos().ativos.length },
      { chave: 'encerrados' as const, quantidade: this.grupos().encerrados.length },
    ]),
  );
  abas = computed<AbaLista[]>(() => {
    const avisos = notificacoesDaLista(this.grupos(), this.ehMentor());
    return ABAS_LISTA_ACOMPANHAMENTOS.map((a) => ({ ...a, notificacao: avisos[a.chave] }));
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

  abrirNovoAcompanhamento() {
    this.novoAcompanhamento()?.abrir();
  }

  // Depois de enviar uma proposta: busca a lista de novo, sem piscar "Carregando..."
  propostaEnviada() {
    this.carregar(false);
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
