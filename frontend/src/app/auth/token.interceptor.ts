import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API_URL } from '../api.config';
import { SessaoService } from './sessao.service';

// Passa por toda requisição HTTP do app antes de ela sair:
// se houver login, coloca o token no cabeçalho "Authorization: Bearer <token>".
// Só para chamadas à nossa API, para o token nunca ir para outros sites.
export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const sessao = inject(SessaoService);
  const router = inject(Router);
  const token = sessao.token();

  const ehNossaApi = req.url.startsWith(API_URL);
  // Login e cadastro não precisam de token (e um 401 neles é senha errada, não token vencido)
  const ehLoginOuCadastro = req.url.startsWith(`${API_URL}/auth/login`) || req.url.startsWith(`${API_URL}/auth/cadastro`);
  const comToken = token && ehNossaApi && !ehLoginOuCadastro
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(comToken).pipe(
    catchError((e: HttpErrorResponse) => {
      // Token vencido ou inválido: encerra a sessão e manda para o login
      if (e.status === 401 && comToken !== req) {
        sessao.sair();
        router.navigateByUrl('/login');
      }
      return throwError(() => e);
    }),
  );
};
