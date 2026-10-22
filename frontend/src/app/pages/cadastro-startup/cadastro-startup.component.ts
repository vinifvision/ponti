import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { AuthCabecalhoComponent } from '../../layout/auth-layout/auth-cabecalho.component';
import {
  NIVEL_MAXIMO,
  NOMES_NIVEIS,
  PERGUNTAS_MATURIDADE,
  calcularNivel,
} from './maturidade';

// Cadastro da startup: questionário de maturidade (uma pergunta por vez) seguido da tela de resultado.
// Um "Não" encerra o questionário; o nível é o da última resposta "Sim" consecutiva.
@Component({
  selector: 'app-cadastro-startup',
  standalone: true,
  imports: [AuthCabecalhoComponent, FormsModule],
  templateUrl: './cadastro-startup.component.html',
})
export class CadastroStartupComponent {
  private router = inject(Router);
  private auth = inject(AuthService);

  readonly perguntas = PERGUNTAS_MATURIDADE;
  readonly niveis = Array.from({ length: NIVEL_MAXIMO }, (_, i) => i + 1);

  // Respostas dadas até agora, na ordem das perguntas.
  respostas = signal<boolean[]>([]);
  // Só tem valor quando o questionário terminou.
  nivel = signal<number | null>(null);
  salvando = signal(false);
  erro = signal('');

  // Depois do resultado do questionário vem a etapa de criar a conta.
  etapaConta = signal(false);
  nome = '';
  usuario = '';
  senha = '';
  segmento = '';
  cidade = '';
  estado = '';
  descricao = '';

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
    const nivel = this.nivel();
    if (nivel === null || this.salvando()) return;

    if (!this.nome.trim() || !this.usuario.trim() || this.senha.length < 6) {
      this.erro.set('Preencha o nome da startup, o usuário e uma senha de pelo menos 6 caracteres.');
      return;
    }

    this.salvando.set(true);
    this.erro.set('');

    this.auth
      .cadastrarStartup({
        usuario: this.usuario,
        senha: this.senha,
        nome: this.nome,
        descricao: this.descricao || undefined,
        segmento: this.segmento || undefined,
        cidade: this.cidade || undefined,
        estado: this.estado || undefined,
        estagioMaturidade: nivel,
      })
      .subscribe({
        next: () => this.router.navigateByUrl('/encontrar-mentores'),
        error: (e: HttpErrorResponse) => {
          this.erro.set(mensagemDeErro(e, 'Não foi possível criar a conta. Confira os dados e tente de novo.'));
          this.salvando.set(false);
        },
      });
  }
}
