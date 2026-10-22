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

export type StatusConvite = 'PENDENTE' | 'ACEITO' | 'RECUSADO';

export interface Convite {
  id?: number;
  startupId: number;
  mentorId: number;
  status?: StatusConvite;
}

export interface ConviteDetalhe {
  id: number;
  status: StatusConvite;
  startup: {
    id: number;
    nome: string;
    descricao?: string;
    segmento?: string;
    cidade?: string;
    estado?: string;
    estagioMaturidade?: number | null;
    videoPitchUrl?: string;
    site?: string | null;
  } | null;
  mentor: {
    id: number;
    nome: string;
    areasAtuacao?: string;
    ecossistemas?: string;
    valorSessao?: number | null;
  } | null;
}

export interface Mentor {
  id?: number;
  nome: string;
  bio?: string;
  areasAtuacao?: string;
  ecossistemas?: string;
  anosExperiencia?: number | null;
  valorSessao?: number | null;
  fotoUrl?: string;
}
