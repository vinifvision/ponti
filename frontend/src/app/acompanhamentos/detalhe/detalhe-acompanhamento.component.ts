import { Component, OnInit, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IconeComponent } from '../../shared/icone/icone.component';
import { SeloComponent } from '../../shared/selo/selo.component';
import { iniciais } from '../../shared/texto.utils';
import { ABAS_ATIVIDADES, dataCurta } from '../acompanhamento.utils';
import { AcompanhamentoAtualService } from './acompanhamento-atual.service';

// Casca da página de um acompanhamento (Tela 3): cabeçalho, aviso de somente leitura e as subabas.
// Cada subaba é uma rota filha, então o botão voltar e o link compartilhável funcionam.
@Component({
  selector: 'app-detalhe-acompanhamento',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, IconeComponent, SeloComponent],
  providers: [AcompanhamentoAtualService],
  templateUrl: './detalhe-acompanhamento.component.html',
})
export class DetalheAcompanhamentoComponent implements OnInit {
  // Vem da rota /acompanhamentos/:id (withComponentInputBinding)
  id = input.required<string>();

  protected atual = inject(AcompanhamentoAtualService);

  readonly iniciais = iniciais;
  // Todas atividades, Pendentes, Atrasadas e Devolvidas (só a primeira mostra a linha do tempo)
  readonly abas = ABAS_ATIVIDADES;

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
}
