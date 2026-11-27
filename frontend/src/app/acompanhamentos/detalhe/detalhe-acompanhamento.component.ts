import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal, viewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { BolinhaNotificacaoComponent } from '../../shared/bolinha-notificacao/bolinha-notificacao.component';
import { DialogoConfirmacaoComponent } from '../../shared/dialogo-confirmacao/dialogo-confirmacao.component';
import { IconeComponent } from '../../shared/icone/icone.component';
import { SeloComponent } from '../../shared/selo/selo.component';
import { iniciais } from '../../shared/texto.utils';
import { Meta } from '../acompanhamento.models';
import { AcompanhamentoService } from '../acompanhamento.service';
import { ABAS_ATIVIDADES, FiltroAtividades, dataCurta, notificacoesDasAbas } from '../acompanhamento.utils';
import { AcompanhamentoAtualService } from './acompanhamento-atual.service';

// Casca da página de um acompanhamento (Tela 3): cabeçalho, metas, encerramento, aviso de somente leitura e as subabas.
// Cada subaba é uma rota filha, então o botão voltar e o link compartilhável funcionam.
@Component({
  selector: 'app-detalhe-acompanhamento',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    FormsModule,
    IconeComponent,
    SeloComponent,
    DialogoConfirmacaoComponent,
    BolinhaNotificacaoComponent,
  ],
  providers: [AcompanhamentoAtualService],
  templateUrl: './detalhe-acompanhamento.component.html',
})
export class DetalheAcompanhamentoComponent implements OnInit {
  // Vem da rota /acompanhamentos/:id (withComponentInputBinding)
  id = input.required<string>();

  protected atual = inject(AcompanhamentoAtualService);
  private service = inject(AcompanhamentoService);
  private dialogoEncerrar = viewChild.required(DialogoConfirmacaoComponent);

  encerrando = signal(false);
  metasExpandidas = signal(false);
  modalMetasAberto = signal(false);
  modalEdicaoAberto = signal(false);
  tituloRascunho = signal('');
  metasRascunho = signal<string[]>([]);
  novaMeta = signal('');
  avisoEdicao = signal('');
  salvandoMetaId = signal<number | null>(null);
  erroAcao = signal('');

  podeEncerrar = computed(() => this.atual.ehMentor() && this.atual.acompanhamento()?.status === 'ATIVO');
  podeMarcarMeta = computed(() => this.atual.ehMentor() && !this.atual.somenteLeitura());
  metasCumpridasCount = computed(() => this.atual.metas().filter((m) => m.cumprida).length);

  readonly iniciais = iniciais;
  // Todas atividades, Pendentes, Atrasadas e Devolvidas (só a primeira mostra a linha do tempo)
  readonly abas = ABAS_ATIVIDADES;

  // Abas com algo esperando a pessoa ganham a bolinha de aviso (veja notificacoesDasAbas)
  notificacoes = computed(() =>
    notificacoesDasAbas(this.atual.atividades(), this.atual.ehMentor(), this.atual.somenteLeitura()),
  );
  readonly textoNotificacao: Record<FiltroAtividades, string> = {
    todas: 'Há entregas para revisar',
    pendentes: 'Há atividades pendentes',
    atrasadas: 'Há atividades atrasadas',
    devolvidas: 'Há atividades devolvidas',
  };

  abertura = computed(() => {
    const a = this.atual.acompanhamento();
    if (!a) return '';
    if (a.status === 'PROPOSTO') return 'Proposta aguardando resposta';
    const aberto = `Aberto em ${dataCurta(a.dataInicio)}`;
    return a.status === 'ENCERRADO' ? `${aberto} · encerrado em ${dataCurta(a.dataFim)}` : aberto;
  });

  selo = computed(() => {
    switch (this.atual.acompanhamento()?.status) {
      case 'ATIVO':
        return { texto: 'Ativo', tom: 'sucesso' } as const;
      case 'ENCERRADO':
        return { texto: 'Encerrado', tom: 'neutro' } as const;
      default:
        return { texto: 'Proposta pendente', tom: 'aviso' } as const;
    }
  });

  aviso = computed(() => {
    const a = this.atual.acompanhamento();
    if (a?.status === 'ENCERRADO') {
      return `Este acompanhamento foi encerrado em ${dataCurta(a.dataFim)}. Você pode consultar tudo o que foi feito, mas não é possível fazer alterações.`;
    }
    if (a?.status === 'PROPOSTO') {
      return 'Esta proposta ainda não foi aceita. As atividades ficam disponíveis depois do aceite.';
    }
    return '';
  });

  ngOnInit() {
    this.atual.carregar(Number(this.id()));
  }

  alternarMeta(meta: Meta) {
    if (!this.podeMarcarMeta() || this.salvandoMetaId() === meta.id || meta.id < 0 || this.avisoEdicao()) return;
    this.salvandoMetaId.set(meta.id);
    this.erroAcao.set('');
    this.service.atualizarMeta(meta.id, !meta.cumprida).subscribe({
      next: (atualizada) => {
        this.atual.substituirMeta(atualizada);
        this.salvandoMetaId.set(null);
      },
      error: (e: HttpErrorResponse) => {
        this.erroAcao.set(mensagemDeErro(e, 'Não foi possível atualizar a meta. Tente de novo.'));
        this.salvandoMetaId.set(null);
      },
    });
  }

  alternarMetas() { this.metasExpandidas.update((v) => !v); }

  abrirEdicaoMetas(event?: Event) {
    event?.stopPropagation();
    this.metasRascunho.set(this.atual.metas().map((m) => m.titulo));
    this.modalMetasAberto.set(true);
  }

  abrirEdicaoAcompanhamento() {
    const a = this.atual.acompanhamento();
    if (!a) return;
    this.tituloRascunho.set(a.objetivo);
    this.metasRascunho.set(this.atual.metas().map((m) => m.titulo));
    this.modalEdicaoAberto.set(true);
  }

  adicionarMetaRascunho() {
    const titulo = this.novaMeta().trim();
    if (!titulo) return;
    this.metasRascunho.update((lista) => [...lista, titulo]);
    this.novaMeta.set('');
  }

  removerMetaRascunho(indice: number) { this.metasRascunho.update((lista) => lista.filter((_, i) => i !== indice)); }

  private aplicarMetasLocais() {
    const atuais = this.atual.metas();
    this.atual.metas.set(this.metasRascunho().map((titulo, i) => ({ id: atuais[i]?.id ?? -(i + 1), titulo, cumprida: atuais[i]?.cumprida ?? false, cumpridaEm: atuais[i]?.cumpridaEm ?? null })));
  }

  salvarEdicaoLocal() {
    const a = this.atual.acompanhamento();
    if (!a || !this.tituloRascunho().trim()) return;
    this.atual.acompanhamento.set({ ...a, objetivo: this.tituloRascunho().trim() });
    this.aplicarMetasLocais();
    this.modalEdicaoAberto.set(false);
    this.avisoEdicao.set('Alterações aplicadas apenas nesta visualização. A API atual não oferece operações para salvar título ou metas.');
  }

  salvarMetasLocais() {
    this.aplicarMetasLocais();
    this.modalMetasAberto.set(false);
    this.avisoEdicao.set('Alterações aplicadas apenas nesta visualização. A API atual não oferece operações para salvar metas.');
  }

  async encerrarAcompanhamento() {
    const ac = this.atual.acompanhamento();
    if (!ac || !this.podeEncerrar() || this.encerrando()) return;
    const confirmou = await this.dialogoEncerrar().perguntar();
    if (!confirmou) return;
    this.encerrando.set(true);
    this.erroAcao.set('');
    this.service.encerrar(ac.id).subscribe({
      next: (resumo) => {
        this.atual.atualizarResumo(resumo);
        this.encerrando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erroAcao.set(mensagemDeErro(e, 'Não foi possível encerrar o acompanhamento. Tente de novo.'));
        this.encerrando.set(false);
      },
    });
  }
}
