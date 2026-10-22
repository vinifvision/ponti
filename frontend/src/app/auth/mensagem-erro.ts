import { HttpErrorResponse } from '@angular/common/http';

// Pega a mensagem que o backend mandou (ex.: "Este e-mail já está cadastrado").
// A mensagem genérica de validação do Spring ("Validation failed for object...") não ajuda
// quem está usando o sistema, então nesse caso fica a mensagem padrão da tela.
export function mensagemDeErro(e: HttpErrorResponse, padrao: string): string {
  if (e.status === 0) return 'Não foi possível falar com o servidor. Confira se a API está no ar.';
  const msg = e.error?.message;
  if (typeof msg !== 'string' || !msg.trim() || msg.startsWith('Validation failed')) return padrao;
  return msg;
}
