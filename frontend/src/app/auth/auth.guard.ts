import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessaoService } from './sessao.service';

// Só deixa entrar na área logada quem já fez login; os outros vão para a tela de login.
export const autenticadoGuard: CanActivateFn = () => {
  const sessao = inject(SessaoService);
  const router = inject(Router);
  return sessao.logado() ? true : router.createUrlTree(['/login']);
};
