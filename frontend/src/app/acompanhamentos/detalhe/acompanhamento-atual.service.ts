import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { SessaoService } from '../../auth/sessao.service';
import { AcompanhamentoService } from '../acompanhamento.service';
import { Meta, ResumoAcompanhamento } from '../acompanhamento.models';

// O acompanhamento aberto na página. É fornecido pela casca da página (não é global): a casca carrega
// uma vez e todas as subabas perguntam aqui quem está vendo e se pode editar.
@Injectable()
export class AcompanhamentoAtualService {
  private api = inject(AcompanhamentoService);
  private sessao = inject(SessaoService);

  readonly acompanhamento = signal<ResumoAcompanhamento | null>(null);
  readonly metas = signal<Meta[]>([]);
  readonly carregando = signal(true);
  readonly erro = signal('');

  readonly ehMentor = computed(() => this.sessao.tipo() === 'MENTOR');
  // Só dá para alterar acompanhamento ativo; encerrado (e proposta ainda sem aceite) é somente leitura
  readonly somenteLeitura = computed(() => this.acompanhamento()?.status !== 'ATIVO');

  carregar(id: number) {
    this.carregando.set(true);
    this.erro.set('');
    this.api.detalhe(id).subscribe({
      next: (a) => {
        this.acompanhamento.set(a);
        this.carregando.set(false);
        this.carregarMetas(id);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(
          e.status === 404 ? 'Acompanhamento não encontrado.' : mensagemDeErro(e, 'Não foi possível carregar o acompanhamento.'),
        );
        this.carregando.set(false);
      },
    });
  }

  carregarMetas(id: number) {
    this.api.metas(id).subscribe({
      next: (lista) => this.metas.set(lista),
      error: () => {
        // Falha silenciosa: o resumo continua exibindo os totais agregados
      },
    });
  }

  atualizarResumo(resumo: ResumoAcompanhamento) {
    this.acompanhamento.set(resumo);
  }

  substituirMeta(meta: Meta) {
    this.metas.update((lista) => lista.map((m) => (m.id === meta.id ? meta : m)));
    this.acompanhamento.update((a) => {
      if (!a) return a;
      const cumpridas = this.metas().filter((m) => m.cumprida).length;
      return { ...a, metasCumpridas: cumpridas, metasTotal: this.metas().length };
    });
  }
}
