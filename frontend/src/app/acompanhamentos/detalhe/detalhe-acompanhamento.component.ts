import { Component, OnInit, computed, inject, input, viewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IconeComponent } from '../../shared/icone/icone.component';
import { SeloComponent } from '../../shared/selo/selo.component';
import { iniciais } from '../../shared/texto.utils';
import { dataCurta } from '../acompanhamento.utils';
import { ModalNovoAcompanhamentoComponent } from '../componentes/modal-novo-acompanhamento.component';
import { AcompanhamentoAtualService } from './acompanhamento-atual.service';

// Casca da página de um acompanhamento (Tela 3): cabeçalho, aviso de somente leitura e as subabas.
// O mentor ainda tem aqui o botão "Novo acompanhamento" (escolhe uma startup elegível e abre a proposta).
// Cada subaba é uma rota filha, então o botão voltar e o link compartilhável funcionam.
@Component({
  selector: 'app-detalhe-acompanhamento',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, IconeComponent, SeloComponent, ModalNovoAcompanhamentoComponent],
  providers: [AcompanhamentoAtualService],
  templateUrl: './detalhe-acompanhamento.component.html',
})
export class DetalheAcompanhamentoComponent implements OnInit {
  // Vem da rota /acompanhamentos/:id (withComponentInputBinding)
  id = input.required<string>();

  protected atual = inject(AcompanhamentoAtualService);
  // Janela do mentor para propor um novo acompanhamento (só existe na tela do mentor)
  protected novoAcompanhamento = viewChild.required<ModalNovoAcompanhamentoComponent>('novo');

  readonly iniciais = iniciais;
  readonly abas = [
    { rotulo: 'Visão geral', rota: 'visao-geral' },
    { rotulo: 'Atividades', rota: 'atividades' },
    { rotulo: 'Relatórios', rota: 'relatorios' },
    { rotulo: 'Reuniões', rota: 'reunioes' },
  ];

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
