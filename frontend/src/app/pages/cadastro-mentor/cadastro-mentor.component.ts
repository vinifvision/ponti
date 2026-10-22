import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
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

  nome = '';
  bio = '';
  areasAtuacao = '';
  ecossistemas = '';
  valorSessao: number | null = null;
  usuario = '';
  senha = '';

  salvando = signal(false);
  erro = signal('');

  criarConta() {
    if (this.salvando()) return;

    if (!this.nome.trim() || !this.usuario.trim() || this.senha.length < 6) {
      this.erro.set('Preencha o nome, o usuário e uma senha de pelo menos 6 caracteres.');
      return;
    }

    this.salvando.set(true);
    this.erro.set('');

    this.auth
      .cadastrarMentor({
        usuario: this.usuario,
        senha: this.senha,
        nome: this.nome,
        bio: this.bio || undefined,
        areasAtuacao: this.areasAtuacao || undefined,
        ecossistemas: this.ecossistemas || undefined,
        valorSessao: this.valorSessao ?? undefined,
      })
      .subscribe({
        next: () => this.router.navigateByUrl('/convites'),
        error: (e: HttpErrorResponse) => {
          this.erro.set(mensagemDeErro(e, 'Não foi possível criar a conta. Confira os dados e tente de novo.'));
          this.salvando.set(false);
        },
      });
  }
}
