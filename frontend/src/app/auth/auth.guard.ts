import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { decidirAcesso, rotaInicialDe } from './acesso';
import { SessaoService, TipoUsuario } from './sessao.service';

function verificar(permitidos: readonly TipoUsuario[]) {
  const sessao = inject(SessaoService);
  const router = inject(Router);

  const decisao = decidirAcesso(sessao.logado(), sessao.tipo(), permitidos);
  if (decisao === 'permitir') return true;
  return router.createUrlTree([decisao === 'ir-para-login' ? '/login' : rotaInicialDe(sessao.tipo())]);
}

// Só deixa entrar na área logada quem já fez login; os outros vão para a tela de login.
export const autenticadoGuard: CanActivateFn = () => verificar([]);

// Além do login, exige que a conta seja de um dos tipos informados.
// Ex.: canActivate: [tipoGuard('MENTOR')]. Quem é do outro tipo volta para a tela inicial dele.
export const tipoGuard =
  (...permitidos: TipoUsuario[]): CanActivateFn =>
  () =>
    verificar(permitidos);
