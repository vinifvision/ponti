import type { TipoUsuario } from '../auth/sessao.service';

// Formatos das respostas da área do admin (iguais aos DTOs do backend: DashboardAdmin, UsuarioAdmin, PaginaAdmin)

export interface Pagina<T> {
  itens: T[];
  total: number;
  pagina: number;
  tamanho: number;
}

export interface UsuarioAdmin {
  id: number;
  email: string;
  tipo: TipoUsuario;
  nome: string | null;
  perfilId: number | null;
  bloqueado: boolean;
  bloqueadoEm: string | null;
  motivoBloqueio: string | null;
  criadoEm: string | null;
  ultimoAcessoEm: string | null;
}

export type AcaoAuditoria = 'BLOQUEAR_USUARIO' | 'DESBLOQUEAR_USUARIO';

export interface RegistroAuditoria {
  id: number;
  autorUsuarioId: number;
  autorEmail: string;
  acao: AcaoAuditoria;
  alvoUsuarioId: number | null;
  alvoDescricao: string | null;
  motivo: string | null;
  criadoEm: string;
}

export type Etapa = 'CADASTRO' | 'INTERESSE' | 'MATCH' | 'MENTORIA' | 'ACOMPANHAMENTO';
export type Faixa = 'ALTA' | 'MEDIA' | 'BAIXA';
export type Situacao = 'FALTA' | 'EQUILIBRIO' | 'SOBRA';
export type TipoAtencao = 'MENTOR_MAL_AVALIADO' | 'MENTOR_SEM_RESPOSTA' | 'STARTUP_SEM_CONEXAO';

export interface Dashboard {
  periodo: { inicio: string; fim: string; anteriorInicio: string; anteriorFim: string };
  destaques: { chave: string; rotulo: string; atual: number; anterior: number }[];
  funil: { etapas: EtapaFunil[]; maiorQueda: Etapa | null };
  match: { faixas: FaixaMatch[]; diferenca: number | null };
  ofertaDemanda: TemaOfertaDemanda[];
  saude: Saude;
  atencao: Atencao[];
}

export interface EtapaFunil {
  etapa: Etapa;
  rotulo: string;
  quantidade: number;
  taxaDaAnterior: number | null;
  taxaDoInicio: number | null;
  quantidadeAnterior: number;
  taxaDaAnteriorNoPeriodoAnterior: number | null;
}

export interface FaixaMatch {
  faixa: Faixa;
  rotulo: string;
  pares: number;
  convertidos: number;
  taxa: number | null;
  paresAnterior: number;
  taxaAnterior: number | null;
}

export interface TemaOfertaDemanda {
  tema: string;
  rotulo: string;
  startups: number;
  mentores: number;
  situacao: Situacao;
}

export interface Saude {
  ativos: number;
  entregasNoPrazo: number;
  entregasAtrasadas: number;
  atividadesVencidas: number;
  percentualNoPrazo: number | null;
  parados: { acompanhamentoId: number; startup: string; mentor: string; diasSemMovimento: number }[];
}

export interface Atencao {
  tipo: TipoAtencao;
  usuarioId: number;
  perfil: TipoUsuario;
  perfilId: number;
  nome: string;
  motivo: string;
  bloqueado: boolean;
}
