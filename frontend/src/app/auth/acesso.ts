import type { TipoUsuario } from './sessao.service';

// Tela inicial de cada tipo de conta: startup procura mentores; mentor procura startups.
export function rotaInicialDe(tipo: TipoUsuario | null): string {
  return tipo === 'MENTOR' ? '/encontrar-startups' : '/encontrar-mentores';
}

export type DecisaoDeAcesso = 'permitir' | 'ir-para-login' | 'ir-para-inicio';

// Regra dos guards, sem depender do Angular (fica fácil de testar):
//  - sem login: vai para o login;
//  - logado, mas de um tipo que não pode ver a tela: volta para a tela inicial dele;
//  - se "permitidos" estiver vazio, qualquer pessoa logada entra.
export function decidirAcesso(
  logado: boolean,
  tipo: TipoUsuario | null,
  permitidos: readonly TipoUsuario[] = [],
): DecisaoDeAcesso {
  if (!logado) return 'ir-para-login';
  if (permitidos.length > 0 && (tipo === null || !permitidos.includes(tipo))) return 'ir-para-inicio';
  return 'permitir';
}
