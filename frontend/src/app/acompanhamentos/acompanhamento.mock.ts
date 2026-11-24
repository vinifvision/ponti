import { dataHoraIso, dataIso } from '../shared/data.utils';
import {
  Anexo,
  Atividade,
  Comentario,
  Entrega,
  NovaAtividade,
  NovaEntrega,
  NovaProposta,
  PainelConsolidado,
  PapelAutor,
  ResumoAcompanhamento,
  StatusAtividade,
} from './acompanhamento.models';
import { ordenarPorPrazo, percentual, resumirAtividades } from './acompanhamento.utils';

// TEMPORÁRIO!!: "banco de dados" de mentira, em memória, para montar as telas antes de o backend do
// módulo existir. As datas são relativas a hoje, para sempre haver exemplo de atividade atrasada.
// Apagar este arquivo quando o AcompanhamentoService passar a chamar a API de verdade.

export type TipoMock = 'MENTOR' | 'STARTUP';

// Erro com o código HTTP que a API de verdade devolveria (o service transforma em HttpErrorResponse)
export class ErroMock extends Error {
  constructor(
    public status: number,
    mensagem: string,
  ) {
    super(mensagem);
  }
}

interface Registro {
  resumo: ResumoAcompanhamento;
  // null: usa os números fixos do resumo (propostas e itens sem detalhe)
  atividades: Atividade[] | null;
}

const copiar = <T>(valor: T): T => JSON.parse(JSON.stringify(valor)) as T;

function quando(agora: Date, dias: number, hora = '23:59'): string {
  const d = new Date(agora);
  d.setDate(d.getDate() + dias);
  return `${dataIso(d)}T${hora}:00`;
}

function atividade(
  acId: number, n: number, titulo: string, descricao: string, prazo: string,
  status: StatusAtividade, entregas: Entrega[] = [], anexos: Anexo[] = [],
): Atividade {
  return { id: acId * 100 + n, acompanhamentoId: acId, titulo, descricao, prazo, anexos, status, entregas };
}

function entrega(
  id: number, texto: string, link: string | null, enviadaEm: string,
  feedback: string | null = null, comentarios: Comentario[] = [], anexos: Anexo[] = [],
): Entrega {
  return { id, texto, link, anexos, enviadaEm, feedback, comentarios };
}

// Arquivo de exemplo (sem endereço: aparece como anexo, sem link para baixar)
const arquivo = (nome: string, tamanho: number): Anexo => ({ nome, tamanho, url: null });

// Os arquivos que a pessoa escolheu viram anexos. O endereço é temporário (vale enquanto a página
// estiver aberta); a API de verdade devolve o endereço do arquivo guardado.
function paraAnexos(arquivos?: File[]): Anexo[] {
  return (arquivos ?? []).map((f) => ({ nome: f.name, tamanho: f.size, url: URL.createObjectURL(f) }));
}

function comentario(id: number, autor: PapelAutor, nomeAutor: string, texto: string, criadoEm: string): Comentario {
  return { id, autor, nomeAutor, texto, criadoEm };
}

// Plano principal: mostra todos os estados (aprovada, aguardando revisão, devolvida, atrasada e pendente)
function planoPrincipal(acId: number, agora: Date, nomeStartup: string, nomeMentor: string): Atividade[] {
  return [
    atividade(acId, 1, 'Mapear e agendar 10 entrevistas',
      'Liste potenciais clientes do seu segmento e agende conversas de 30 minutos para entender como resolvem o problema hoje.',
      quando(agora, -14), 'APROVADA',
      [entrega(acId * 10 + 1, 'Listei 14 contatos e já agendei 10 conversas para as próximas semanas.',
        'https://docs.google.com/spreadsheets/d/exemplo', quando(agora, -15, '10:20'),
        'Ótima lista! Priorize quem já usa uma solução parecida.',
        [comentario(acId * 10 + 1, 'MENTOR', nomeMentor, 'Podemos conversar sobre o roteiro na próxima reunião?', quando(agora, -14, '09:00')),
         comentario(acId * 10 + 2, 'STARTUP', nomeStartup, 'Pode ser! Levo as primeiras perguntas.', quando(agora, -14, '11:40'))],
        [arquivo('Lista_de_contatos.pdf', 248_000)])],
      [arquivo('Modelo_de_roteiro.pdf', 512_000), arquivo('Guia_de_entrevistas.pdf', 1_300_000)]),
    atividade(acId, 2, 'Consolidar os aprendizados das entrevistas',
      'Resuma os padrões que apareceram nas conversas: dores mais citadas, alternativas usadas hoje e objeções.',
      quando(agora, -5), 'ENTREGUE',
      [entrega(acId * 10 + 3, 'Resumi os principais padrões e as 3 dores mais citadas, com trechos das falas.',
        'https://notion.so/exemplo', quando(agora, -6, '15:30'), null,
        [comentario(acId * 10 + 4, 'STARTUP', nomeStartup, 'Se preferir, posso apresentar rapidinho na reunião.', quando(agora, -6, '15:45'))])]),
    atividade(acId, 3, 'Desenhar o mapa da proposta de valor',
      'Monte o mapa com dores, ganhos e alívios para cada segmento de cliente que você identificou.',
      quando(agora, -2), 'DEVOLVIDA',
      [entrega(acId * 10 + 5, 'Primeira versão do mapa, ainda com um único segmento.', null, quando(agora, -3, '18:05'),
        'Faltou separar os ganhos por segmento de cliente. Revise e reenvie.')]),
    atividade(acId, 4, 'Escrever as hipóteses a validar',
      'Transforme o que aprendeu em hipóteses testáveis, cada uma com o critério que mostra se estava certa.',
      quando(agora, -1), 'PENDENTE'),
    atividade(acId, 5, 'Validar a proposta de valor com 3 clientes',
      'Apresente a proposta a pelo menos 3 clientes e registre o que mudaria no produto.',
      quando(agora, 7), 'PENDENTE'),
  ];
}

function planoSecundario(acId: number, agora: Date, nomeStartup: string): Atividade[] {
  return [
    atividade(acId, 1, 'Listar as exigências regulatórias do produto',
      'Levante as normas que se aplicam ao produto e o que cada uma exige da startup.',
      quando(agora, -10), 'APROVADA',
      [entrega(acId * 10 + 1, `${nomeStartup} levantou 6 normas e o que cada uma exige.`, null, quando(agora, -11, '14:10'))]),
    atividade(acId, 2, 'Montar o checklist da primeira auditoria',
      'Organize em um checklist tudo o que será pedido na primeira auditoria.',
      quando(agora, 4), 'PENDENTE'),
    atividade(acId, 3, 'Simular a auditoria com o mentor',
      'Ensaie a auditoria com o mentor e anote os pontos fracos.',
      quando(agora, 12), 'PENDENTE'),
  ];
}

function planoEncerrado(acId: number, agora: Date): Atividade[] {
  return [60, 45, 30].map((dias, i) =>
    atividade(acId, i + 1, ['Modelar a receita', 'Projetar os custos fixos', 'Montar o fluxo de caixa'][i],
      'Atividade concluída durante o acompanhamento.', quando(agora, -dias), 'APROVADA',
      [entrega(acId * 10 + i + 1, 'Entrega concluída.', null, quando(agora, -dias - 1, '16:00'))]),
  );
}

function fixo(
  id: number, status: ResumoAcompanhamento['status'], objetivo: string, parceiro: ResumoAcompanhamento['parceiro'],
  extra: Partial<ResumoAcompanhamento> = {},
): ResumoAcompanhamento {
  return {
    id, status, objetivo, dataInicio: null, dataFim: null, parceiro,
    atividadesAprovadas: 0, atividadesTotal: 0, metasCumpridas: 0, metasTotal: 0,
    proximoPrazo: null, atividadesAtrasadas: 0, pendencias: 0,
    ...extra,
  };
}

function sementes(tipo: TipoMock, agora: Date, meuNome: string): Registro[] {
  const mentor = tipo === 'MENTOR';
  const [p1, p2, p3, p4] = mentor
    ? [
        { id: 1, nome: 'NutriPet', fotoUrl: null, subtitulo: 'Nível 4 · Prova de conceito pronta' },
        { id: 2, nome: 'VetTech', fotoUrl: null, subtitulo: 'Nível 5 · Versão básica testada' },
        { id: 3, nome: 'AgroLeve', fotoUrl: null, subtitulo: 'Nível 3 · Pesquisa e design ativos' },
        { id: 4, nome: 'ClinApp', fotoUrl: null, subtitulo: 'Nível 6 · Protótipo funcional' },
      ]
    : [
        { id: 1, nome: 'Ricardo Silva', fotoUrl: null, subtitulo: 'Gestão, Aceleração' },
        { id: 2, nome: 'Juliana G.', fotoUrl: null, subtitulo: 'Zootecnista, Professora' },
        { id: 3, nome: 'Patrícia Nunes', fotoUrl: null, subtitulo: 'Regulatório em saúde' },
        { id: 4, nome: 'Marcos Lima', fotoUrl: null, subtitulo: 'Finanças, Estratégia' },
      ];
  const base = mentor ? 200 : 100;
  // Quem escreveu cada lado: a pessoa logada e a outra ponta
  const nomes = (parceiro: string) => (mentor ? { startup: parceiro, mentor: meuNome } : { startup: meuNome, mentor: parceiro });
  const n2 = nomes(p2.nome);
  const n3 = nomes(p3.nome);

  return [
    { resumo: fixo(base + 1, 'PROPOSTO',
        mentor ? 'Preparar a startup para a primeira rodada de testes com clínicas.' : 'Validar o MVP e fechar os 3 primeiros clientes piloto.',
        p1, { metasSugeridas: mentor ? 2 : 3 }), atividades: null },
    { resumo: fixo(base + 2, 'ATIVO', 'Estruturar o discovery e validar a proposta de valor.', p2,
        { dataInicio: quando(agora, -70).slice(0, 10), metasCumpridas: 2, metasTotal: 4 }),
      atividades: planoPrincipal(base + 2, agora, n2.startup, n2.mentor) },
    { resumo: fixo(base + 3, 'ATIVO', mentor ? 'Definir o modelo de negócio e o primeiro piloto.' : 'Organizar o regulatório e preparar a primeira auditoria.', p3,
        { dataInicio: quando(agora, -45).slice(0, 10), metasCumpridas: 1, metasTotal: 3 }),
      atividades: planoSecundario(base + 3, agora, n3.startup) },
    { resumo: fixo(base + 4, 'ENCERRADO', mentor ? 'Preparar o pitch para a rodada de investimento anjo.' : 'Montar o plano de negócios e o primeiro modelo financeiro.', p4,
        { dataInicio: quando(agora, -150).slice(0, 10), dataFim: quando(agora, -20).slice(0, 10), atividadesAprovadas: 3, atividadesTotal: 3, metasCumpridas: 4, metasTotal: 4 }),
      atividades: planoEncerrado(base + 4, agora) },
  ];
}

export class BancoMock {
  private registros: Registro[];
  private contador = 90_000;

  constructor(
    private tipo: TipoMock,
    private meuNome: string,
    private agora: () => Date = () => new Date(),
  ) {
    this.registros = sementes(tipo, agora(), meuNome);
  }

  private get ehMentor() {
    return this.tipo === 'MENTOR';
  }

  private novoId() {
    return ++this.contador;
  }

  private resumir(r: Registro): ResumoAcompanhamento {
    const resumo = copiar(r.resumo);
    return r.atividades ? { ...resumo, ...resumirAtividades(r.atividades, this.ehMentor, this.agora()) } : resumo;
  }

  private registro(id: number): Registro {
    const r = this.registros.find((x) => x.resumo.id === id);
    if (!r) throw new ErroMock(404, 'Acompanhamento não encontrado.');
    return r;
  }

  private registroAtivo(id: number): Registro {
    const r = this.registro(id);
    if (r.resumo.status !== 'ATIVO') throw new ErroMock(409, 'Este acompanhamento não está ativo.');
    return r;
  }

  private achar(atividadeId: number): { registro: Registro; atividade: Atividade } {
    for (const registro of this.registros) {
      const atividade = registro.atividades?.find((a) => a.id === atividadeId);
      if (atividade) return { registro, atividade };
    }
    throw new ErroMock(404, 'Atividade não encontrada.');
  }

  private exigir(papel: TipoMock) {
    if (this.tipo !== papel) throw new ErroMock(403, 'Você não tem permissão para esta ação.');
  }

  private agoraIso() {
    return dataHoraIso(this.agora());
  }

  // ----- consultas -----

  listar(): ResumoAcompanhamento[] {
    return this.registros.map((r) => this.resumir(r));
  }

  painel(): PainelConsolidado {
    const ativos = this.listar().filter((a) => a.status === 'ATIVO');
    const soma = (campo: 'atividadesAprovadas' | 'atividadesTotal' | 'metasCumpridas' | 'metasTotal') =>
      ativos.reduce((total, a) => total + a[campo], 0);
    const total = soma('atividadesTotal');
    const atrasadas = ativos.reduce((t, a) => t + a.atividadesAtrasadas, 0);
    return {
      acompanhamentosAtivos: ativos.length,
      atividadesAprovadas: soma('atividadesAprovadas'),
      atividadesTotal: total,
      entregasNoPrazo: total ? percentual(total - atrasadas, total) : null,
      metasCumpridas: soma('metasCumpridas'),
      metasTotal: soma('metasTotal'),
    };
  }

  detalhe(id: number): ResumoAcompanhamento {
    return this.resumir(this.registro(id));
  }

  atividades(id: number): Atividade[] {
    return copiar(ordenarPorPrazo(this.registro(id).atividades ?? []));
  }

  // ----- propostas -----

  propor(nova: NovaProposta): ResumoAcompanhamento {
    this.exigir('MENTOR');
    const existe = this.registros.some(
      (r) => r.resumo.parceiro.id === nova.startupId && (r.resumo.status === 'PROPOSTO' || r.resumo.status === 'ATIVO'),
    );
    if (existe) throw new ErroMock(409, 'Já existe uma proposta ou um acompanhamento em andamento com esta startup.');
    const metas = nova.metas.map((m) => m.trim()).filter(Boolean);
    const registro: Registro = {
      resumo: fixo(this.novoId(), 'PROPOSTO', nova.objetivo.trim(),
        { id: nova.startupId, nome: nova.nomeStartup ?? `Startup ${nova.startupId}`, fotoUrl: null, subtitulo: null },
        { metasSugeridas: metas.length }),
      atividades: null,
    };
    this.registros.unshift(registro);
    return this.resumir(registro);
  }

  aceitar(id: number) {
    this.exigir('STARTUP');
    const r = this.registro(id);
    if (r.resumo.status !== 'PROPOSTO') throw new ErroMock(409, 'Esta proposta não está mais pendente.');
    r.resumo.status = 'ATIVO';
    r.resumo.dataInicio = dataIso(this.agora());
    r.resumo.metasTotal = r.resumo.metasSugeridas ?? 0;
    r.atividades = [];
  }

  recusar(id: number) {
    this.exigir('STARTUP');
    const r = this.registro(id);
    if (r.resumo.status !== 'PROPOSTO') throw new ErroMock(409, 'Esta proposta não está mais pendente.');
    this.registros = this.registros.filter((x) => x !== r);
  }

  // ----- atividades, entregas e comentários -----

  criarAtividade(acompanhamentoId: number, dados: NovaAtividade): Atividade {
    this.exigir('MENTOR');
    const r = this.registroAtivo(acompanhamentoId);
    if (!dados.titulo.trim()) throw new ErroMock(400, 'Informe o título da atividade.');
    const nova: Atividade = {
      id: this.novoId(), acompanhamentoId, titulo: dados.titulo.trim(), descricao: dados.descricao.trim(),
      prazo: dados.prazo, anexos: paraAnexos(dados.arquivos), status: 'PENDENTE', entregas: [],
    };
    (r.atividades ??= []).push(nova);
    return copiar(nova);
  }

  entregar(atividadeId: number, dados: NovaEntrega): Atividade {
    this.exigir('STARTUP');
    const { registro, atividade } = this.achar(atividadeId);
    this.registroAtivo(registro.resumo.id);
    if (atividade.status !== 'PENDENTE' && atividade.status !== 'DEVOLVIDA') {
      throw new ErroMock(409, 'Esta atividade não aceita uma nova entrega agora.');
    }
    const texto = dados.texto.trim();
    if (!texto && !dados.link && !dados.arquivos?.length) throw new ErroMock(400, 'Escreva um texto, informe um link ou anexe um arquivo.');
    atividade.entregas.push({
      id: this.novoId(), texto, link: dados.link, anexos: paraAnexos(dados.arquivos),
      enviadaEm: this.agoraIso(), feedback: null, comentarios: [],
    });
    atividade.status = 'ENTREGUE';
    return copiar(atividade);
  }

  // A startup desfaz o envio enquanto o mentor ainda não avaliou. Sai a última entrega; se havia uma entrega
  // anterior (já devolvida), a atividade volta a "devolvida", senão a "pendente".
  desfazerEntrega(atividadeId: number): Atividade {
    this.exigir('STARTUP');
    const { registro, atividade } = this.achar(atividadeId);
    this.registroAtivo(registro.resumo.id);
    if (atividade.status !== 'ENTREGUE') throw new ErroMock(409, 'Só dá para desfazer um envio que o mentor ainda não avaliou.');
    atividade.entregas.pop();
    atividade.status = atividade.entregas.length ? 'DEVOLVIDA' : 'PENDENTE';
    return copiar(atividade);
  }

  aprovar(atividadeId: number, feedback?: string): Atividade {
    return this.avaliar(atividadeId, 'APROVADA', feedback?.trim() || null);
  }

  devolver(atividadeId: number, feedback: string): Atividade {
    if (!feedback.trim()) throw new ErroMock(400, 'Explique o que precisa ser ajustado.');
    return this.avaliar(atividadeId, 'DEVOLVIDA', feedback.trim());
  }

  private avaliar(atividadeId: number, status: 'APROVADA' | 'DEVOLVIDA', feedback: string | null): Atividade {
    this.exigir('MENTOR');
    const { registro, atividade } = this.achar(atividadeId);
    this.registroAtivo(registro.resumo.id);
    if (atividade.status !== 'ENTREGUE') throw new ErroMock(409, 'Não há entrega para avaliar.');
    atividade.entregas[atividade.entregas.length - 1].feedback = feedback;
    atividade.status = status;
    return copiar(atividade);
  }

  comentar(atividadeId: number, entregaId: number, texto: string): Atividade {
    const { registro, atividade } = this.achar(atividadeId);
    this.registroAtivo(registro.resumo.id);
    const alvo = atividade.entregas.find((e) => e.id === entregaId);
    if (!alvo) throw new ErroMock(404, 'Entrega não encontrada.');
    if (!texto.trim()) throw new ErroMock(400, 'Escreva o comentário.');
    alvo.comentarios.push({ id: this.novoId(), autor: this.tipo, nomeAutor: this.meuNome, texto: texto.trim(), criadoEm: this.agoraIso() });
    return copiar(atividade);
  }
}
