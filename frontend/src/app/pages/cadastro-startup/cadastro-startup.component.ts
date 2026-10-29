import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { SessaoService } from '../../auth/sessao.service';
import { SENHA_MINIMA, validarConta } from '../../auth/validacao-cadastro';
import { AuthCabecalhoComponent } from '../../layout/auth-layout/auth-cabecalho.component';
import { StartupService } from '../../startups/startup.service';
import { SeletorTemasComponent } from '../../shared/seletor-temas/seletor-temas.component';
import {
  NIVEL_MAXIMO,
  NOMES_NIVEIS,
  PERGUNTAS_MATURIDADE,
  calcularNivel,
} from '../../shared/maturidade';

// Cadastro da startup em três etapas:
// 1. questionário de maturidade (uma pergunta por vez);
// 2. resultado com o nível calculado;
// 3. criação da conta (nome, área de atuação, sobre, e-mail e senha).
// O cadastro na API não recebe o nível: logo depois de criar a conta (e entrar),
// o nível é gravado na startup com um PUT separado.
// Um "Não" encerra o questionário; o nível é o da última resposta "Sim" consecutiva.
@Component({
  selector: 'app-cadastro-startup',
  standalone: true,
  imports: [AuthCabecalhoComponent, FormsModule, SeletorTemasComponent],
  templateUrl: './cadastro-startup.component.html',
})
export class CadastroStartupComponent {
  private router = inject(Router);
  private auth = inject(AuthService);
  private sessao = inject(SessaoService);
  private startupService = inject(StartupService);

  readonly perguntas = PERGUNTAS_MATURIDADE;
  readonly niveis = Array.from({ length: NIVEL_MAXIMO }, (_, i) => i + 1);
  readonly senhaMinima = SENHA_MINIMA;

  // Respostas dadas até agora, na ordem das perguntas.
  respostas = signal<boolean[]>([]);
  // Só tem valor quando o questionário terminou.
  nivel = signal<number | null>(null);

  // Depois do resultado do questionário vem a etapa de criar a conta.
  etapaConta = signal(false);
  nome = '';
  areaAtuacao = '';
  sobre = '';
  email = '';
  senha = '';
  necessidades = signal<string[]>([]);

  // Fica true quando a conta já foi criada mas o nível ainda não foi gravado (ex.: a rede caiu).
  // Assim, tentar de novo só regrava o nível, em vez de tentar criar a conta outra vez.
  contaCriada = signal(false);
  salvando = signal(false);
  erro = signal('');

  indice = computed(() => this.respostas().length);
  perguntaAtual = computed(() => this.perguntas[this.indice()]);
  progresso = computed(() => (this.indice() / this.perguntas.length) * 100);
  nomeNivel = computed(() => NOMES_NIVEIS[this.nivel() ?? 1]);

  responder(sim: boolean) {
    const respostas = [...this.respostas(), sim];
    this.respostas.set(respostas);

    // Terminou se respondeu "Não" ou se respondeu a última pergunta.
    if (!sim || respostas.length === this.perguntas.length) {
      this.nivel.set(calcularNivel(respostas));
    }
  }

  voltarPergunta() {
    this.respostas.update((r) => r.slice(0, -1));
  }

  refazer() {
    this.respostas.set([]);
    this.nivel.set(null);
    this.erro.set('');
  }

  continuar() {
    if (this.nivel() === null) return;
    this.erro.set('');
    this.etapaConta.set(true);
  }

  voltarParaResultado() {
    this.erro.set('');
    this.etapaConta.set(false);
  }

  criarConta() {
    if (this.nivel() === null || this.salvando()) return;

    if (this.contaCriada()) {
      this.salvarNivel();
      return;
    }

    const campos = { nome: this.nome, areaAtuacao: this.areaAtuacao, sobre: this.sobre, email: this.email, senha: this.senha };
    const erro = validarConta(campos);
    if (erro) {
      this.erro.set(erro);
      return;
    }
    if (this.necessidades().length === 0) {
      this.erro.set('Escolha pelo menos um tema em que a sua startup precisa de ajuda.');
      return;
    }

    this.salvando.set(true);
    this.erro.set('');

    this.auth.cadastrarStartup({ ...campos, necessidades: this.necessidades() }).subscribe({
      // Conta criada e login feito: agora grava o nível do questionário
      next: () => {
        this.contaCriada.set(true);
        this.salvarNivel();
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível criar a conta. Confira os dados e tente de novo.'));
        this.salvando.set(false);
      },
    });
  }

  // Grava o nível na startup da conta recém-criada e entra no sistema
  private salvarNivel() {
    const nivel = this.nivel();
    const startupId = this.sessao.startupId();
    if (nivel === null || startupId === null) return;

    this.salvando.set(true);
    this.erro.set('');

    this.startupService.atualizarMaturidade(startupId, nivel).subscribe({
      next: () => this.router.navigateByUrl('/encontrar-mentores'),
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Sua conta foi criada, mas não foi possível salvar o nível. Tente de novo.'));
        this.salvando.set(false);
      },
    });
  }
}
