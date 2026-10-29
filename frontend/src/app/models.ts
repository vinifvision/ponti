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
  // Ainda não existe no backend: o perfil só mostra a seção "Redes de contato" quando a API passar a mandar este campo
  linkedinUrl?: string | null;
  estagioMaturidade?: number | null;
  // Temas em que a startup precisa de ajuda (códigos de GET /temas)
  necessidades?: string[];
  // Só nas listas "recomendadas" do mentor: compatibilidade (0 a 100) e o porquê
  compatibilidade?: number;
  motivos?: string[];
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
  } | null;
}

export interface Mentor {
  id?: number;
  nome: string;
  bio?: string;
  areasAtuacao?: string;
  ecossistemas?: string;
  // Temas em que o mentor ajuda (códigos de GET /temas)
  temas?: string[];
  // Só na lista "recomendados" da startup: compatibilidade (0 a 100) e o porquê
  compatibilidade?: number;
  motivos?: string[];
  anosExperiencia?: number | null;
  valorSessao?: number | null;
  duracaoSessaoMinutos?: number | null;
  fotoUrl?: string | null;
  videoUrl?: string | null;
  linkedinUrl?: string | null;
  // Reputação calculada pelo backend
  notaMedia?: number | null;
  totalAvaliacoes?: number;
  totalMentorias?: number;
}

// Avaliação que aparece no perfil do mentor
export interface AvaliacaoPublica {
  id: number;
  nota: number;
  comentario?: string | null;
  startupNome: string;
  startupFotoUrl?: string | null;
  criadoEm: string;
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

// ---------- Agenda (fluxo estilo Preply) ----------

export type DiaSemana = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

// Faixa semanal em que o mentor atende. Horas no formato "09:00" (o backend devolve "09:00:00").
export interface FaixaDisponibilidade {
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFim: string;
}

// Faixa de horas dentro de uma data específica ("14:00" a "16:00")
export interface FaixaHorario {
  horaInicio: string;
  horaFim: string;
}

// Ajuste de UMA data: vale no lugar do horário semanal daquele dia (não atende, ou atende só nessas faixas).
// sessoesMarcadas = mentorias pendentes ou confirmadas já marcadas nessa data.
export interface ExcecaoDia {
  data: string; // "2026-10-29"
  indisponivel: boolean;
  faixas: FaixaHorario[];
  sessoesMarcadas: number;
}

export interface DefinicaoExcecao {
  indisponivel: boolean;
  faixas: FaixaHorario[];
}

export type StatusAgendamento = 'PENDENTE' | 'CONFIRMADO' | 'RECUSADO' | 'CANCELADO';
export type StatusPagamento = 'GRATUITA' | 'AGUARDANDO' | 'PAGO' | 'REEMBOLSADO' | 'EXPIRADO';
export type MetodoPagamento = 'PIX' | 'CARTAO';

export interface NovoAgendamento {
  startupId: number;
  mentorId: number;
  inicio: string; // "2026-11-03T09:00:00", horário de Recife
  metodoPagamento?: MetodoPagamento;
  cartao?: DadosCartao;
  mensagem?: string;
}

// Vai só para o gateway simulado; o backend guarda apenas a bandeira e os 4 últimos dígitos
export interface DadosCartao {
  numero: string;
  nomeImpresso: string;
  validade: string; // "MM/AA"
  cvv: string;
}

export interface PagamentoAgendamento {
  codigoTransacao: string;
  pixCopiaECola?: string | null; // só enquanto o Pix está AGUARDANDO
  expiraEm?: string | null;
  pagoEm?: string | null;
  cartaoFinal?: string | null;
  cartaoBandeira?: string | null;
}

export interface ParteAgendamento {
  id: number;
  nome: string;
  fotoUrl?: string | null;
  descricao?: string | null; // segmento da startup ou área do mentor
}

export interface Agendamento {
  id: number;
  inicio: string;
  fim: string;
  valor: number;
  status: StatusAgendamento;
  statusPagamento: StatusPagamento;
  metodoPagamento?: MetodoPagamento | null;
  linkReuniao?: string | null; // só quando CONFIRMADO
  mensagem?: string | null;
  startup: ParteAgendamento | null;
  mentor: ParteAgendamento | null;
  realizada?: boolean; // confirmada e já aconteceu
  pagamento?: PagamentoAgendamento | null;
  avaliacao?: { nota: number; comentario?: string | null } | null;
}
