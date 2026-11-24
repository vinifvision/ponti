import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { SessaoService } from '../../auth/sessao.service';
import { AcompanhamentoService } from '../acompanhamento.service';
import { ResumoAcompanhamento } from '../acompanhamento.models';

// O acompanhamento aberto na página. É fornecido pela casca da página (não é global): a casca carrega
// uma vez e todas as subabas perguntam aqui quem está vendo e se pode editar.
@Injectable()
export class AcompanhamentoAtualService {
  private api = inject(AcompanhamentoService);
  private sessao = inject(SessaoService);

  readonly acompanhamento = signal<ResumoAcompanhamento | null>(null);
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
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(
          e.status === 404 ? 'Acompanhamento não encontrado.' : mensagemDeErro(e, 'Não foi possível carregar o acompanhamento.'),
        );
        this.carregando.set(false);
      },
    });
  }
}
