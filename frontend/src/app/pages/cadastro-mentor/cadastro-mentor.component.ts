import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { SENHA_MAXIMA, SENHA_MINIMA } from '../../auth/validacao-cadastro';
import { AuthCabecalhoComponent } from '../../layout/auth-layout/auth-cabecalho.component';
import { Certificacao } from '../../models';
import { SeletorTemasComponent } from '../../shared/seletor-temas/seletor-temas.component';
import { formatarPeriodo, formatarValorSessao, idVideoYoutube, linkSeguro } from '../../shared/mentoria.utils';

const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const ANO_ATUAL = new Date().getFullYear();

// Cadastro do mentor em 4 etapas, uma por tela. Só a última envia para o backend,
// que cria o perfil e a conta de uma vez e já devolve a sessão.
@Component({
  selector: 'app-cadastro-mentor',
  standalone: true,
  imports: [AuthCabecalhoComponent, FormsModule, SeletorTemasComponent],
  templateUrl: './cadastro-mentor.component.html',
})
export class CadastroMentorComponent {
  private router = inject(Router);
  private auth = inject(AuthService);

  readonly senhaMinima = SENHA_MINIMA;
  readonly etapas = ['Conta', 'Perfil profissional', 'Vitrine', 'Mentoria'];
  readonly duracoes = [30, 45, 60, 90];
  readonly formatarPeriodo = formatarPeriodo;

  etapa = signal(0);
  progresso = computed(() => ((this.etapa() + 1) / this.etapas.length) * 100);

  // Etapa 1: conta
  nome = '';
  email = '';
  senha = '';

  // Etapa 2: perfil profissional
  areaAtuacao = '';
  ecossistemas = '';
  temas = signal<string[]>([]);
  anosExperiencia: number | null = null;
  sobre = '';
  linkedinUrl = '';

  // Etapa 3: vitrine (foto, vídeo e certificações)
  fotoUrl = '';
  videoUrl = '';
  certificacoes = signal<Certificacao[]>([]);
  novaCertificacao: Certificacao = this.certificacaoVazia();

  // Etapa 4: como é a mentoria
  gratuita = false;
  valorSessao: number | null = null;
  duracaoSessaoMinutos = 60;

  salvando = signal(false);
  erro = signal('');

  avancar() {
    const erro = this.validarEtapa(this.etapa());
    if (erro) {
      this.erro.set(erro);
      return;
    }
    this.erro.set('');
    this.etapa.update((e) => e + 1);
  }

  voltar() {
    this.erro.set('');
    this.etapa.update((e) => Math.max(0, e - 1));
  }

  adicionarCertificacao() {
    const c = this.novaCertificacao;
    if (!c.titulo.trim()) {
      this.erro.set('Informe o título da certificação ou experiência.');
      return;
    }
    if (!this.anoValido(c.anoInicio) || !this.anoValido(c.anoFim)) {
      this.erro.set(`Use anos entre 1950 e ${ANO_ATUAL + 10}.`);
      return;
    }
    if (c.anoInicio && c.anoFim && c.anoFim < c.anoInicio) {
      this.erro.set('O ano de término não pode ser antes do ano de início.');
      return;
    }
    if (this.certificacoes().length >= 20) {
      this.erro.set('Você pode adicionar até 20 certificações.');
      return;
    }
    this.erro.set('');
    this.certificacoes.update((lista) => [...lista, { ...c, titulo: c.titulo.trim() }]);
    this.novaCertificacao = this.certificacaoVazia();
  }

  removerCertificacao(indice: number) {
    this.certificacoes.update((lista) => lista.filter((_, i) => i !== indice));
  }

  resumoValor(): string {
    return formatarValorSessao(this.gratuita ? 0 : this.valorSessao);
  }

  criarConta() {
    if (this.salvando()) return;

    for (let i = 0; i < this.etapas.length; i++) {
      const erro = this.validarEtapa(i);
      if (erro) {
        this.etapa.set(i);
        this.erro.set(erro);
        return;
      }
    }

    this.salvando.set(true);
    this.erro.set('');

    this.auth
      .cadastrarMentor({
        nome: this.nome.trim(),
        email: this.email.trim(),
        senha: this.senha,
        areaAtuacao: this.areaAtuacao.trim(),
        ecossistemas: this.opcional(this.ecossistemas),
        temas: this.temas(),
        anosExperiencia: this.anosExperiencia!,
        sobre: this.sobre.trim(),
        linkedinUrl: this.opcional(linkSeguro(this.linkedinUrl) ?? ''),
        fotoUrl: this.opcional(linkSeguro(this.fotoUrl) ?? ''),
        videoUrl: this.opcional(this.videoUrl),
        certificacoes: this.certificacoes(),
        valorSessao: this.gratuita ? 0 : this.valorSessao!,
        duracaoSessaoMinutos: this.duracaoSessaoMinutos,
      })
      .subscribe({
        next: () => this.router.navigate(['/agenda'], { queryParams: { novo: 1 } }),
        error: (e: HttpErrorResponse) => {
          this.erro.set(mensagemDeErro(e, 'Não foi possível criar a conta. Confira os dados e tente de novo.'));
          this.salvando.set(false);
        },
      });
  }

  // Mesmas regras do backend (CadastroMentor), conferidas antes para mostrar a mensagem certa.
  private validarEtapa(etapa: number): string {
    switch (etapa) {
      case 0:
        if (!this.nome.trim()) return 'Informe o seu nome.';
        if (this.nome.length > 255) return 'O nome pode ter no máximo 255 caracteres.';
        if (!EMAIL_VALIDO.test(this.email.trim())) return 'Informe um e-mail válido.';
        if (this.senha.length < SENHA_MINIMA) return `A senha precisa ter pelo menos ${SENHA_MINIMA} caracteres.`;
        if (this.senha.length > SENHA_MAXIMA) return `A senha pode ter no máximo ${SENHA_MAXIMA} caracteres.`;
        return '';
      case 1: {
        const anos = this.anosExperiencia;
        if (!this.areaAtuacao.trim()) return 'Informe a sua área de atuação.';
        if (this.temas().length === 0) return 'Escolha pelo menos um tema em que você ajuda as startups.';
        if (anos === null || !Number.isInteger(anos) || anos < 0 || anos > 70)
          return 'Informe o tempo de experiência em anos (de 0 a 70).';
        if (!this.sobre.trim()) return 'Conte um pouco sobre você no campo "Sobre mim".';
        if (this.sobre.length > 2000) return 'O campo "Sobre mim" pode ter no máximo 2000 caracteres.';
        if (this.linkedinUrl.trim() && !linkSeguro(this.linkedinUrl)) return 'O link do LinkedIn não parece válido.';
        return '';
      }
      case 2:
        if (this.fotoUrl.trim() && !linkSeguro(this.fotoUrl)) return 'O link da foto não parece válido.';
        if (this.videoUrl.trim() && !idVideoYoutube(this.videoUrl))
          return 'Use um link do YouTube para o vídeo (ex.: https://www.youtube.com/watch?v=...).';
        return '';
      case 3: {
        if (this.gratuita) return '';
        const valor = this.valorSessao;
        if (valor === null || Number.isNaN(valor) || valor <= 0)
          return 'Informe o valor por sessão ou marque "Mentoria gratuita".';
        if (valor > 100000) return 'O valor por sessão pode ser no máximo R$ 100.000.';
        if (Math.round(valor * 100) !== valor * 100) return 'Use no máximo 2 casas decimais no valor.';
        return '';
      }
      default:
        return '';
    }
  }

  private anoValido(ano: number | null | undefined): boolean {
    return ano === null || ano === undefined || (Number.isInteger(ano) && ano >= 1950 && ano <= ANO_ATUAL + 10);
  }

  private opcional(texto: string): string | undefined {
    const limpo = texto.trim();
    return limpo ? limpo : undefined;
  }

  private certificacaoVazia(): Certificacao {
    return { titulo: '', instituicao: '', anoInicio: null, anoFim: null, descricao: '' };
  }
}
