import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SessaoService } from '../../auth/sessao.service';
import { AuthCabecalhoComponent } from '../../layout/auth-layout/auth-cabecalho.component';
import { StartupService } from '../../startups/startup.service';
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
  imports: [AuthCabecalhoComponent],
  templateUrl: './cadastro-startup.component.html',
})
export class CadastroStartupComponent {
  private router = inject(Router);
  private sessao = inject(SessaoService);
  private startupService = inject(StartupService);

  readonly perguntas = PERGUNTAS_MATURIDADE;
  readonly niveis = Array.from({ length: NIVEL_MAXIMO }, (_, i) => i + 1);

  // Respostas dadas até agora, na ordem das perguntas.
  respostas = signal<boolean[]>([]);
  // Só tem valor quando o questionário terminou.
  nivel = signal<number | null>(null);
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
    const nivel = this.nivel();
    if (nivel === null || this.salvando()) return;

    this.salvando.set(true);
    this.erro.set('');

    // TEMPORÁRIO!!: ainda não existe cadastro de verdade, então o nível é gravado na startup
    // da sessão (SessaoService). Quando o cadastro criar a startup, gravar o nível junto com ela.
    this.startupService.atualizarMaturidade(this.sessao.startupId(), nivel).subscribe({
      next: () => this.router.navigateByUrl('/encontrar-mentores'),
      error: () => {
        this.erro.set('Não foi possível salvar o nível. Confira se a API está no ar e tente de novo.');
        this.salvando.set(false);
      },
    });
  }
}
