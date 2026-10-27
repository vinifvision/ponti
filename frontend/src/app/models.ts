export interface Startup {
  id?: number;
  nome: string;
  descricao?: string;
  videoPitchUrl?: string;
  fotoUrl?: string | null;
  atuacoes?: string;
  segmento?: string;
  cidade?: string;
  estado?: string;
  site?: string;
  estagioMaturidade?: number | null;
}

export type StatusConvite = 'PENDENTE' | 'ACEITO' | 'RECUSADO';

// Quem tomou a iniciativa do convite. Quem responde é sempre o outro lado.
export type LadoConvite = 'STARTUP' | 'MENTOR';

export interface Convite {
  id?: number;
  startupId: number;
  mentorId: number;
  status?: StatusConvite;
  enviadoPor?: LadoConvite;
}

export interface ConviteDetalhe {
  id: number;
  status: StatusConvite;
  enviadoPor: LadoConvite;
  startup: {
    id: number;
    nome: string;
    descricao?: string;
    segmento?: string;
    cidade?: string;
    estado?: string;
    estagioMaturidade?: number | null;
    videoPitchUrl?: string;
    fotoUrl?: string | null;
    site?: string | null;
  } | null;
  mentor: {
    id: number;
    nome: string;
    areasAtuacao?: string;
    ecossistemas?: string;
    valorSessao?: number | null;
    duracaoSessaoMinutos?: number | null;
    fotoUrl?: string | null;
    linkedinUrl?: string | null;
    // Só vem preenchido depois que o convite é aceito
    whatsapp?: string | null;
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
  duracaoSessaoMinutos?: number | null;
  fotoUrl?: string | null;
  videoUrl?: string | null;
  linkedinUrl?: string | null;
  // O WhatsApp nunca vem nas listagens: só em ConviteDetalhe, depois do aceite.
  whatsapp?: string | null;
}

// Certificação ou experiência que aparece no perfil
export interface Certificacao {
  id?: number;
  titulo: string;
  instituicao?: string | null;
  anoInicio?: number | null;
  anoFim?: number | null;
  descricao?: string | null;
}
