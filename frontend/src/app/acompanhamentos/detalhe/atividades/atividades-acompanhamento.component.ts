import { Component, computed, inject, input, viewChild } from '@angular/core';
import { PerfilLogadoService } from '../../../auth/perfil-logado.service';
import { SessaoService } from '../../../auth/sessao.service';
import { Atividade, PessoaAvatar } from '../../acompanhamento.models';
import { FiltroAtividades, ehFiltroAtividades, filtrarAtividades } from '../../acompanhamento.utils';
import { ModalAtividadeComponent } from '../../componentes/modal-atividade.component';
import { ModalFeedbackComponent } from '../../componentes/modal-feedback.component';
import { PassoAtividadeComponent } from '../../componentes/passo-atividade.component';
import { AcompanhamentoAtualService } from '../acompanhamento-atual.service';

// Abas "Todas atividades", "Pendentes", "Atrasadas" e "Devolvidas" (a rota manda o filtro).
//   Todas atividades: linha do tempo vertical com um passo por atividade, do prazo mais próximo ao mais distante.
//   As outras três: só os cartões das atividades do filtro, em lista simples, sem a linha do tempo.
// A startup entrega; o mentor cria atividades e aprova ou devolve as entregas.
@Component({
  selector: 'app-atividades-acompanhamento',
  standalone: true,
  imports: [PassoAtividadeComponent, ModalAtividadeComponent, ModalFeedbackComponent],
  templateUrl: './atividades-acompanhamento.component.html',
})
export class AtividadesAcompanhamentoComponent {
  private atual = inject(AcompanhamentoAtualService);
  private sessao = inject(SessaoService);
  private perfilLogado = inject(PerfilLogadoService);

  // Vem da rota /acompanhamentos/:id/:filtro (withComponentInputBinding)
  filtro = input<string>('todas');

  private modalAtividade = viewChild.required(ModalAtividadeComponent);
  private modalFeedback = viewChild.required(ModalFeedbackComponent);

  readonly acompanhamento = this.atual.acompanhamento;
  readonly ehMentor = this.atual.ehMentor;
  readonly somenteLeitura = this.atual.somenteLeitura;

  // O carregamento é feito pela casca da página (AcompanhamentoAtualService), que também usa as atividades
  // para mostrar a bolinha de aviso nas abas
  readonly atividades = this.atual.atividades;
  readonly carregando = this.atual.carregandoAtividades;
  readonly erro = this.atual.erroAtividades;

  filtroAtual = computed<FiltroAtividades>(() => {
    const f = this.filtro();
    return ehFiltroAtividades(f) ? f : 'todas';
  });
  // A linha do tempo (bolinhas numeradas e a linha entre os passos) só existe na aba "Todas atividades"
  linhaDoTempo = computed(() => this.filtroAtual() === 'todas');
  visiveis = computed(() => filtrarAtividades(this.atividades(), this.filtroAtual()));
  textoVazioDoFiltro = computed(() => {
    switch (this.filtroAtual()) {
      case 'pendentes':
        return 'Nenhuma atividade pendente.';
      case 'atrasadas':
        return 'Nenhuma atividade atrasada.';
      case 'devolvidas':
        return 'Nenhuma atividade devolvida.';
      default:
        return '';
    }
  });
  aprovadas = computed(() => this.atividades().filter((a) => a.status === 'APROVADA').length);
  // Quem escreve as entregas: a startup (a pessoa logada, ou o parceiro quando quem vê é o mentor)
  nomeStartup = computed(() =>
    this.ehMentor() ? (this.acompanhamento()?.parceiro.nome ?? 'Startup') : (this.sessao.nome() ?? 'Sua startup'),
  );

  // Quem aprova ou devolve: o mentor logado, ou o mentor do acompanhamento quando quem vê é a startup.
  // Dá nome e foto ao avatar que aparece junto ao comentário da aprovação ou devolução.
  mentor = computed<PessoaAvatar>(() => {
    if (this.ehMentor()) {
      return { nome: this.perfilLogado.nome() ?? this.sessao.nome() ?? 'Mentor', fotoUrl: this.perfilLogado.foto() };
    }
    const parceiro = this.acompanhamento()?.parceiro;
    return { nome: parceiro?.nome ?? 'Mentor', fotoUrl: parceiro?.fotoUrl ?? null };
  });

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
    this.atual.adicionarAtividade(a);
  }

  substituir(a: Atividade) {
    this.atual.substituirAtividade(a);
  }
}
