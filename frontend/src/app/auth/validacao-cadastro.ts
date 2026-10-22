// Mesmas regras do backend (CadastroStartup / CadastroMentor), conferidas antes de enviar
// para a pessoa ver a mensagem certa em vez de um erro genérico da API.

export const SENHA_MINIMA = 8;
export const SENHA_MAXIMA = 72;

const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export interface CamposConta {
  nome: string;
  areaAtuacao: string;
  sobre: string;
  email: string;
  senha: string;
}

// Devolve a primeira mensagem de erro, ou '' se estiver tudo certo.
export function validarConta(c: CamposConta): string {
  if (!c.nome.trim()) return 'Informe o nome.';
  if (c.nome.length > 255) return 'O nome pode ter no máximo 255 caracteres.';
  if (!c.areaAtuacao.trim()) return 'Informe a área de atuação.';
  if (c.areaAtuacao.length > 255) return 'A área de atuação pode ter no máximo 255 caracteres.';
  if (!c.sobre.trim()) return 'Conte um pouco sobre você no campo "Sobre".';
  if (c.sobre.length > 2000) return 'O campo "Sobre" pode ter no máximo 2000 caracteres.';
  if (!EMAIL_VALIDO.test(c.email.trim())) return 'Informe um e-mail válido.';
  if (c.senha.length < SENHA_MINIMA) return `A senha precisa ter pelo menos ${SENHA_MINIMA} caracteres.`;
  if (c.senha.length > SENHA_MAXIMA) return `A senha pode ter no máximo ${SENHA_MAXIMA} caracteres.`;
  return '';
}
