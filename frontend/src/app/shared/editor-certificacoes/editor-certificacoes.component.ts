import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { CertificacaoService, DonoCertificacao } from '../../certificacoes/certificacao.service';
import { Certificacao } from '../../models';
import { formatarPeriodo } from '../mentoria.utils';

const ANO_MAXIMO = new Date().getFullYear() + 10;
const LIMITE = 20;

// Lista, adiciona e remove as certificações do perfil (usado no "Editar perfil" da startup e do mentor).
// Cada ação já vai direto para a API, sem depender do botão "Salvar alterações" do formulário.
@Component({
  selector: 'app-editor-certificacoes',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './editor-certificacoes.component.html',
})
export class EditorCertificacoesComponent implements OnInit {
  dono = input.required<DonoCertificacao>();
  donoId = input.required<number>();
  titulo = input('Premiações e certificações');
  ajuda = input('Programas de aceleração, prêmios, cursos e certificados importantes (opcional).');

  private servico = inject(CertificacaoService);

  readonly formatarPeriodo = formatarPeriodo;
  lista = signal<Certificacao[]>([]);
  carregando = signal(true);
  salvando = signal(false);
  removendo = signal<number | null>(null);
  erro = signal('');
  nova: Certificacao = this.vazia();

  ngOnInit() {
    this.servico.listar(this.dono(), this.donoId()).subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar as certificações.'));
        this.carregando.set(false);
      },
    });
  }

  adicionar() {
    if (this.salvando()) return;
    const c = this.nova;
    const titulo = (c.titulo ?? '').trim();
    if (!titulo) {
      this.erro.set('Informe o título (ex.: 1º Lugar Demoday).');
      return;
    }
    if (!this.anoValido(c.anoInicio) || !this.anoValido(c.anoFim)) {
      this.erro.set(`Use anos entre 1950 e ${ANO_MAXIMO}.`);
      return;
    }
    if (c.anoInicio && c.anoFim && c.anoFim < c.anoInicio) {
      this.erro.set('O ano de término não pode ser antes do ano de início.');
      return;
    }
    if (this.lista().length >= LIMITE) {
      this.erro.set(`Você pode adicionar até ${LIMITE} itens.`);
      return;
    }

    this.erro.set('');
    this.salvando.set(true);
    const pedido: Certificacao = {
      titulo,
      instituicao: this.opcional(c.instituicao),
      anoInicio: c.anoInicio || null,
      anoFim: c.anoFim || null,
      descricao: this.opcional(c.descricao),
    };
    this.servico.criar(this.dono(), this.donoId(), pedido).subscribe({
      next: (salva) => {
        // Mais recentes primeiro, como a API lista
        this.lista.update((atual) => [salva, ...atual]);
        this.nova = this.vazia();
        this.salvando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.erro.set(mensagemDeErro(e, 'Não foi possível adicionar. Confira os campos.'));
      },
    });
  }

  remover(c: Certificacao) {
    if (c.id == null || this.removendo() !== null) return;
    this.erro.set('');
    this.removendo.set(c.id);
    this.servico.remover(this.dono(), this.donoId(), c.id).subscribe({
      next: () => {
        this.lista.update((atual) => atual.filter((x) => x.id !== c.id));
        this.removendo.set(null);
      },
      error: (e: HttpErrorResponse) => {
        this.removendo.set(null);
        this.erro.set(mensagemDeErro(e, 'Não foi possível remover.'));
      },
    });
  }

  private anoValido(ano: number | null | undefined): boolean {
    return ano == null || (Number.isInteger(ano) && ano >= 1950 && ano <= ANO_MAXIMO);
  }

  private opcional(texto: string | null | undefined): string | null {
    const limpo = (texto ?? '').trim();
    return limpo ? limpo : null;
  }

  private vazia(): Certificacao {
    return { titulo: '', instituicao: '', anoInicio: null, anoFim: null, descricao: '' };
  }
}
