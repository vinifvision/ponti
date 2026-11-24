import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { mensagemDeErro } from '../../../auth/mensagem-erro';
import { SessaoService } from '../../../auth/sessao.service';
import { AcompanhamentoService } from '../../acompanhamento.service';
import { Atividade } from '../../acompanhamento.models';
import { ordenarPorPrazo } from '../../acompanhamento.utils';
import { ModalAtividadeComponent } from '../../componentes/modal-atividade.component';
import { ModalFeedbackComponent } from '../../componentes/modal-feedback.component';
import { PassoAtividadeComponent } from '../../componentes/passo-atividade.component';
import { AcompanhamentoAtualService } from '../acompanhamento-atual.service';

// Aba "Atividades": linha do tempo vertical com um passo por atividade, do prazo mais próximo ao mais distante.
// A startup entrega; o mentor cria atividades e aprova ou devolve as entregas.
@Component({
  selector: 'app-atividades-acompanhamento',
  standalone: true,
  imports: [PassoAtividadeComponent, ModalAtividadeComponent, ModalFeedbackComponent],
  templateUrl: './atividades-acompanhamento.component.html',
})
export class AtividadesAcompanhamentoComponent implements OnInit {
  private service = inject(AcompanhamentoService);
  private atual = inject(AcompanhamentoAtualService);
  private sessao = inject(SessaoService);

  private modalAtividade = viewChild.required(ModalAtividadeComponent);
  private modalFeedback = viewChild.required(ModalFeedbackComponent);

  readonly acompanhamento = this.atual.acompanhamento;
  readonly ehMentor = this.atual.ehMentor;
  readonly somenteLeitura = this.atual.somenteLeitura;

  atividades = signal<Atividade[]>([]);
  carregando = signal(true);
  erro = signal('');

  ordenadas = computed(() => ordenarPorPrazo(this.atividades()));
  aprovadas = computed(() => this.atividades().filter((a) => a.status === 'APROVADA').length);
  // Quem escreve as entregas: a startup (a pessoa logada, ou o parceiro quando quem vê é o mentor)
  nomeStartup = computed(() =>
    this.ehMentor() ? (this.acompanhamento()?.parceiro.nome ?? 'Startup') : (this.sessao.nome() ?? 'Sua startup'),
  );

  ngOnInit() {
    const id = this.acompanhamento()?.id;
    if (id === undefined) return;
    this.service.atividades(id).subscribe({
      next: (lista) => {
        this.atividades.set(lista);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar as atividades.'));
        this.carregando.set(false);
      },
    });
  }

  novaAtividade() {
    this.modalAtividade().abrir();
  }

  aprovar(a: Atividade) {
    this.modalFeedback().abrir(a, 'APROVAR');
  }

  devolver(a: Atividade) {
    this.modalFeedback().abrir(a, 'DEVOLVER');
  }

  adicionar(a: Atividade) {
    this.atividades.update((lista) => [...lista, a]);
  }

  substituir(a: Atividade) {
    this.atividades.update((lista) => lista.map((x) => (x.id === a.id ? a : x)));
  }
}
