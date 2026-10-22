import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { SENHA_MINIMA, validarConta } from '../../auth/validacao-cadastro';
import { AuthCabecalhoComponent } from '../../layout/auth-layout/auth-cabecalho.component';

// Cadastro do mentor: cria o perfil e a conta de uma vez e já entra no sistema.
@Component({
  selector: 'app-cadastro-mentor',
  standalone: true,
  imports: [AuthCabecalhoComponent, FormsModule],
  templateUrl: './cadastro-mentor.component.html',
})
export class CadastroMentorComponent {
  private router = inject(Router);
  private auth = inject(AuthService);

  readonly senhaMinima = SENHA_MINIMA;

  nome = '';
  areaAtuacao = '';
  anosExperiencia: number | null = null;
  sobre = '';
  email = '';
  senha = '';

  salvando = signal(false);
  erro = signal('');

  criarConta() {
    if (this.salvando()) return;

    const campos = { nome: this.nome, areaAtuacao: this.areaAtuacao, sobre: this.sobre, email: this.email, senha: this.senha };
    const anos = this.anosExperiencia;
    const erro =
      anos === null || !Number.isInteger(anos) || anos < 0 || anos > 70
        ? 'Informe o tempo de experiência em anos (de 0 a 70).'
        : validarConta(campos);
    if (erro) {
      this.erro.set(erro);
      return;
    }

    this.salvando.set(true);
    this.erro.set('');

    this.auth.cadastrarMentor({ ...campos, anosExperiencia: anos! }).subscribe({
      next: () => this.router.navigateByUrl('/convites'),
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível criar a conta. Confira os dados e tente de novo.'));
        this.salvando.set(false);
      },
    });
  }
}
