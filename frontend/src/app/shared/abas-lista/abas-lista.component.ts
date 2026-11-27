import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BolinhaNotificacaoComponent } from '../bolinha-notificacao/bolinha-notificacao.component';
import { AbaLista } from './abas-lista.utils';

// Abas das telas de lista (Acompanhamentos e Mentorias), com o mesmo visual das abas da tela de atividades.
// A aba escolhida vai para a URL (?aba=ativos): assim o botão voltar, ao sair de um item, devolve a pessoa à mesma aba.
// Quem usa lê o parâmetro "aba" (withComponentInputBinding) e passa a aba ativa para cá.
@Component({
  selector: 'app-abas-lista',
  standalone: true,
  imports: [RouterLink, BolinhaNotificacaoComponent],
  template: `
    <nav class="mt-8 border-b border-gray-200" [attr.aria-label]="rotulo()">
      <ul class="-mb-px flex flex-wrap gap-x-8">
        @for (aba of abas(); track aba.chave) {
          <li>
            <a
              [routerLink]="[]"
              [queryParams]="{ aba: aba.chave }"
              queryParamsHandling="merge"
              [replaceUrl]="true"
              [attr.aria-current]="aba.chave === ativa() ? 'page' : null"
              [class]="aba.chave === ativa() ? CLASSE_ATIVA : CLASSE_INATIVA"
              class="inline-flex items-center gap-2 border-b-2 py-3 text-lg font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-marinho"
            >
              {{ aba.rotulo }}
              @if (aba.notificacao) {
                <app-bolinha-notificacao [rotulo]="'Tem novidades em ' + aba.rotulo" />
              }
            </a>
          </li>
        }
      </ul>
    </nav>
  `,
})
export class AbasListaComponent {
  abas = input.required<AbaLista[]>();
  ativa = input.required<string>();
  // Nome da navegação para leitores de tela
  rotulo = input('Seções da página');

  protected readonly CLASSE_ATIVA = 'border-ponti-roxo text-ponti-roxo';
  protected readonly CLASSE_INATIVA = 'border-transparent text-gray-600 hover:text-ponti-marinho';
}
