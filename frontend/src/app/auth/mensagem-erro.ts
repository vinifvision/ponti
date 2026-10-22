import { HttpErrorResponse } from '@angular/common/http';

// Pega a mensagem que o backend mandou (ex.: "Esse nome de usuário já está em uso").
export function mensagemDeErro(e: HttpErrorResponse, padrao: string): string {
  if (e.status === 0) return 'Não foi possível falar com o servidor. Confira se a API está no ar.';
  const msg = e.error?.message;
  return typeof msg === 'string' && msg.trim() ? msg : padrao;
}
