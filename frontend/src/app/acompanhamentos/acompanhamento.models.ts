// Tipos do módulo de Acompanhamento (contrato proposto para a lista; confirmar com o backend).

export type StatusAcompanhamento = 'PROPOSTO' | 'ATIVO' | 'RECUSADO' | 'ENCERRADO';

// A outra ponta do acompanhamento: o mentor (visão da startup) ou a startup (visão do mentor)
export interface ParceiroAcompanhamento {
  id: number;
  nome: string;
  fotoUrl: string | null;
  subtitulo: string | null;
}

export interface ResumoAcompanhamento {
  id: number;
  status: StatusAcompanhamento;
  objetivo: string;
  dataInicio: string | null;
  dataFim: string | null;
  parceiro: ParceiroAcompanhamento;
  atividadesAprovadas: number;
  atividadesTotal: number;
  metasCumpridas: number;
  metasTotal: number;
  // Só em propostas: quantas metas o mentor sugeriu
  metasSugeridas?: number;
  proximoPrazo: string | null;
  atividadesAtrasadas: number;
  // Startup: atividades a entregar. Mentor: entregas a revisar.
  pendencias: number;
}

// Painel da startup no topo da lista (soma dos acompanhamentos ativos de todos os mentores)
export interface PainelConsolidado {
  acompanhamentosAtivos: number;
  atividadesAprovadas: number;
  atividadesTotal: number;
  // Porcentagem de 0 a 100; null enquanto não houve nenhuma entrega
  entregasNoPrazo: number | null;
  metasCumpridas: number;
  metasTotal: number;
}

export type StatusAtividade = 'PENDENTE' | 'ENTREGUE' | 'APROVADA' | 'DEVOLVIDA';
export type PapelAutor = 'MENTOR' | 'STARTUP';

// Comentário na conversa de uma entrega (não existe na especificação ainda: confirmar com o backend)
export interface Comentario {
  id: number;
  autor: PapelAutor;
  nomeAutor: string;
  texto: string;
  criadoEm: string;
}

// Arquivo anexado (pelo mentor numa atividade ou pela startup numa entrega).
// "url" é o endereço para baixar; pode vir vazio em dados de exemplo (aí o anexo aparece sem link).
export interface Anexo {
  nome: string;
  tamanho: number | null; // em bytes
  url: string | null;
}

// O que a startup enviou. Cada reenvio vira uma entrega nova (histórico): texto, um link e arquivos.
export interface Entrega {
  id: number;
  texto: string;
  link: string | null;
  anexos?: Anexo[];
  enviadaEm: string;
  // Resposta do mentor ao aprovar (opcional) ou devolver (obrigatório)
  feedback: string | null;
  comentarios: Comentario[];
}

// Item do plano criado pelo mentor. "Atrasada" não é status: é calculado pelo prazo.
export interface Atividade {
  id: number;
  acompanhamentoId: number;
  titulo: string;
  descricao: string;
  // "2026-11-19T23:59:00" (sem fuso, como o resto da API)
  prazo: string;
  // Materiais de apoio que o mentor anexou ao criar a atividade
  anexos?: Anexo[];
  status: StatusAtividade;
  entregas: Entrega[];
}

export interface NovaAtividade {
  titulo: string;
  descricao: string;
  prazo: string;
  arquivos?: File[];
}

export interface NovaEntrega {
  texto: string;
  link: string | null;
  arquivos?: File[];
}

export interface NovaProposta {
  startupId: number;
  objetivo: string;
  // Títulos das metas iniciais (opcionais)
  metas: string[];
  // Só para o mock mostrar o nome no card; a API de verdade não usa
  nomeStartup?: string;
}
