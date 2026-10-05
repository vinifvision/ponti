export interface Startup {
  id?: number;
  nome: string;
  descricao?: string;
  videoPitchUrl?: string;
  atuacoes?: string;
  segmento?: string;
  cidade?: string;
  estado?: string;
  site?: string;
  estagioMaturidade?: number | null;
}

export interface Mentor {
  id?: number;
  nome: string;
  bio?: string;
  areasAtuacao?: string;
  ecossistemas?: string;
  valorSessao?: number | null;
}
