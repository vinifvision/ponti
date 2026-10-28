import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { rotaInicialDe } from '../../auth/acesso';
import { AuthService } from '../../auth/auth.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { AuthCabecalhoComponent } from '../../layout/auth-layout/auth-cabecalho.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, AuthCabecalhoComponent],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private router = inject(Router);
  private auth = inject(AuthService);

  email = '';
  senha = '';
  erro = signal('');
  entrando = signal(false);

  entrar(form: NgForm) {
    this.erro.set('');

    if (form.invalid) {
      form.form.markAllAsTouched();
      this.erro.set('Informe seu e-mail e senha.');
      return;
    }

    this.entrando.set(true);
    this.auth.login(this.email, this.senha).subscribe({
      // Startup começa procurando mentores; mentor começa procurando startups.
      next: (sessao) => this.router.navigateByUrl(rotaInicialDe(sessao.usuario.tipo)),
      error: (e: HttpErrorResponse) => {
        this.erro.set(e.status === 401 ? 'E-mail ou senha inválidos.' : mensagemDeErro(e, 'Não foi possível entrar. Tente novamente.'));
        this.entrando.set(false);
      },
    });
  }
}
