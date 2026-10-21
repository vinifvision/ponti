import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthCabecalhoComponent } from '../../layout/auth-layout/auth-cabecalho.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, AuthCabecalhoComponent],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private router = inject(Router);

  usuario = '';
  senha = '';
  erro = signal('');

  entrar(form: NgForm) {
    this.erro.set('');

    if (form.invalid) {
      form.form.markAllAsTouched();
      this.erro.set('Informe seu usuário e senha.');
      return;
    }

    // TEMPORÁRIO!!: o backend ainda não tem login. Quando existir (POST /auth/login),
    // chamar o serviço de autenticação aqui, guardar o token e tratar 401 com this.erro.set(...).
    this.router.navigateByUrl('/encontrar-mentores');
  }
}
