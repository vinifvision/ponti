import { Atividade, ResumoAcompanhamento } from './acompanhamento.models';
import {
  agruparPorStatus, dataCurta, dataHoraCurta, diasDeAtraso, entregueComAtraso, estaAtrasada, ordenarPorPrazo,
  percentual, plural, prazoCurto, resumirAtividades, selosDaAtividade, selosDoCard, textoPrazo, tomDoNo,
} from './acompanhamento.utils';

function resumo(extra: Partial<ResumoAcompanhamento>): ResumoAcompanhamento {
  return {
    id: 1, status: 'ATIVO', objetivo: 'Objetivo', dataInicio: '2026-09-03', dataFim: null,
    parceiro: { id: 1, nome: 'Fulana', fotoUrl: null, subtitulo: null },
    atividadesAprovadas: 0, atividadesTotal: 0, metasCumpridas: 0, metasTotal: 0,
    proximoPrazo: null, atividadesAtrasadas: 0, pendencias: 0,
    ...extra,
  };
}

describe('percentual', () => {
  it('calcula a porcentagem arredondada', () => {
    expect(percentual(5, 8)).toBe(63);
    expect(percentual(8, 14)).toBe(57);
  });
  it('devolve 0 quando não há total e limita em 100', () => {
    expect(percentual(0, 0)).toBe(0);
    expect(percentual(9, 3)).toBe(100);
  });
});

describe('plural', () => {
  it('usa singular só para 1', () => {
    expect(plural(1, 'atividade atrasada', 'atividades atrasadas')).toBe('1 atividade atrasada');
    expect(plural(2, 'atividade atrasada', 'atividades atrasadas')).toBe('2 atividades atrasadas');
    expect(plural(0, 'meta', 'metas')).toBe('0 metas');
  });
});

describe('dataCurta', () => {
  it('mostra dia e mês abreviado sem mudar o dia por causa do fuso', () => {
    expect(dataCurta('2026-11-12')).toBe('12 nov');
    expect(dataCurta('2026-01-01T00:00:00')).toBe('01 jan');
  });
  it('devolve vazio quando não há data ou ela é inválida', () => {
    expect(dataCurta(null)).toBe('');
    expect(dataCurta('')).toBe('');
    expect(dataCurta('lixo')).toBe('');
  });
});

describe('agruparPorStatus', () => {
  it('separa em pendentes, ativos e encerrados e esconde os recusados', () => {
    const g = agruparPorStatus([
      resumo({ id: 1, status: 'PROPOSTO' }),
      resumo({ id: 2, status: 'ATIVO' }),
      resumo({ id: 3, status: 'RECUSADO' }),
      resumo({ id: 4, status: 'ENCERRADO', dataFim: '2026-10-10' }),
    ]);
    expect(g.pendentes.map((a) => a.id)).toEqual([1]);
    expect(g.ativos.map((a) => a.id)).toEqual([2]);
    expect(g.encerrados.map((a) => a.id)).toEqual([4]);
  });
  it('coloca primeiro os ativos que mais precisam de atenção', () => {
    const g = agruparPorStatus([
      resumo({ id: 1, pendencias: 1 }),
      resumo({ id: 2, atividadesAtrasadas: 1 }),
      resumo({ id: 3 }),
    ]);
    expect(g.ativos.map((a) => a.id)).toEqual([2, 1, 3]);
  });
  it('ordena os encerrados do mais recente para o mais antigo', () => {
    const g = agruparPorStatus([
      resumo({ id: 1, status: 'ENCERRADO', dataFim: '2026-06-01' }),
      resumo({ id: 2, status: 'ENCERRADO', dataFim: '2026-10-10' }),
    ]);
    expect(g.encerrados.map((a) => a.id)).toEqual([2, 1]);
  });
});

describe('selosDoCard', () => {
  it('ativo sem pendências está em dia', () => {
    expect(selosDoCard(resumo({}), false)).toEqual([{ texto: 'Em dia', tom: 'sucesso' }]);
  });
  it('mostra atraso e pendência, com texto diferente para startup e mentor', () => {
    const a = resumo({ atividadesAtrasadas: 1, pendencias: 2 });
    expect(selosDoCard(a, false)).toEqual([
      { texto: '1 atividade atrasada', tom: 'perigo' },
      { texto: '2 atividades para entregar', tom: 'destaque' },
    ]);
    expect(selosDoCard(a, true)[1]).toEqual({ texto: '2 entregas para revisar', tom: 'destaque' });
  });
  it('proposta: só o mentor vê o selo de espera', () => {
    expect(selosDoCard(resumo({ status: 'PROPOSTO' }), false)).toEqual([]);
    expect(selosDoCard(resumo({ status: 'PROPOSTO' }), true)).toEqual([{ texto: 'Aguardando resposta da startup', tom: 'aviso' }]);
  });
  it('encerrado é somente leitura', () => {
    expect(selosDoCard(resumo({ status: 'ENCERRADO' }), false)).toEqual([{ texto: 'Somente leitura', tom: 'neutro' }]);
  });
});

// ---------- atividades e linha do tempo ----------

const AGORA = new Date(2026, 10, 20, 12, 0); // 20 nov 2026, 12:00

function atividade(extra: Partial<Atividade>): Atividade {
  return {
    id: 1, acompanhamentoId: 1, titulo: 'Atividade', descricao: '', prazo: '2026-11-25T23:59:00',
    status: 'PENDENTE', entregas: [], ...extra,
  };
}

describe('atraso', () => {
  it('só está atrasada quando o prazo passou e a startup ainda precisa entregar', () => {
    const vencida = '2026-11-19T23:59:00';
    expect(estaAtrasada(atividade({ prazo: vencida, status: 'PENDENTE' }), AGORA)).toBe(true);
    expect(estaAtrasada(atividade({ prazo: vencida, status: 'DEVOLVIDA' }), AGORA)).toBe(true);
    expect(estaAtrasada(atividade({ prazo: vencida, status: 'ENTREGUE' }), AGORA)).toBe(false);
    expect(estaAtrasada(atividade({ prazo: vencida, status: 'APROVADA' }), AGORA)).toBe(false);
    expect(estaAtrasada(atividade({ prazo: '2026-11-25T23:59:00' }), AGORA)).toBe(false);
  });
  it('conta os dias de atraso e nunca fica negativo', () => {
    expect(diasDeAtraso(atividade({ prazo: '2026-11-17T12:00:00' }), AGORA)).toBe(3);
    expect(diasDeAtraso(atividade({ prazo: '2026-11-25T12:00:00' }), AGORA)).toBe(0);
  });
  it('entrega depois do prazo é entrega com atraso', () => {
    const a = atividade({ prazo: '2026-11-19T23:59:00' });
    const entrega = (enviadaEm: string) => ({ id: 1, texto: '', link: null, enviadaEm, feedback: null, comentarios: [] });
    expect(entregueComAtraso(a, entrega('2026-11-20T08:00:00'))).toBe(true);
    expect(entregueComAtraso(a, entrega('2026-11-19T15:30:00'))).toBe(false);
  });
});

describe('textos de data e prazo', () => {
  it('formata data e hora curtas', () => {
    expect(dataHoraCurta('2026-11-19T15:30:00')).toBe('19 nov às 15:30');
    expect(prazoCurto('2026-11-19T23:59:00')).toBe('19 nov, 23:59');
  });
  it('o texto do prazo muda conforme a situação', () => {
    expect(textoPrazo(atividade({ prazo: '2026-11-25T23:59:00' }), AGORA)).toBe('Entregar até 25 nov, 23:59');
    expect(textoPrazo(atividade({ prazo: '2026-11-17T12:00:00' }), AGORA)).toBe('Venceu em 17 nov, 12:00 (há 3 dias)');
    expect(textoPrazo(atividade({ prazo: '2026-11-17T12:00:00', status: 'APROVADA' }), AGORA)).toBe('Prazo: 17 nov, 12:00');
  });
});

describe('selos e bolinha da linha do tempo', () => {
  it('cada status tem seu selo, e o atraso soma um selo de perigo', () => {
    expect(selosDaAtividade(atividade({ status: 'ENTREGUE' }), AGORA)).toEqual([{ texto: 'Aguardando revisão', tom: 'destaque' }]);
    expect(selosDaAtividade(atividade({ status: 'APROVADA' }), AGORA)).toEqual([{ texto: 'Aprovada', tom: 'sucesso' }]);
    expect(selosDaAtividade(atividade({ prazo: '2026-11-19T23:59:00', status: 'DEVOLVIDA' }), AGORA)).toEqual([
      { texto: 'Devolvida para ajustes', tom: 'aviso' },
      { texto: 'Atrasada', tom: 'perigo' },
    ]);
  });
  it('a bolinha mostra aprovada, atrasada, devolvida, entregue ou pendente (nessa ordem de importância)', () => {
    expect(tomDoNo(atividade({ status: 'APROVADA' }), AGORA)).toBe('aprovada');
    expect(tomDoNo(atividade({ status: 'DEVOLVIDA', prazo: '2026-11-19T23:59:00' }), AGORA)).toBe('atrasada');
    expect(tomDoNo(atividade({ status: 'DEVOLVIDA' }), AGORA)).toBe('devolvida');
    expect(tomDoNo(atividade({ status: 'ENTREGUE' }), AGORA)).toBe('entregue');
    expect(tomDoNo(atividade({}), AGORA)).toBe('pendente');
  });
});

describe('ordenarPorPrazo e resumirAtividades', () => {
  const lista = [
    atividade({ id: 1, prazo: '2026-11-28T23:59:00', status: 'PENDENTE' }),
    atividade({ id: 2, prazo: '2026-11-10T23:59:00', status: 'APROVADA' }),
    atividade({ id: 3, prazo: '2026-11-18T23:59:00', status: 'PENDENTE' }),
    atividade({ id: 4, prazo: '2026-11-15T23:59:00', status: 'ENTREGUE' }),
    atividade({ id: 5, prazo: '2026-11-23T23:59:00', status: 'DEVOLVIDA' }),
  ];
  it('ordena do prazo mais próximo ao mais distante sem mexer na lista original', () => {
    expect(ordenarPorPrazo(lista).map((a) => a.id)).toEqual([2, 4, 3, 5, 1]);
    expect(lista[0].id).toBe(1);
  });
  it('resume as atividades para a startup (a entregar) e para o mentor (a revisar)', () => {
    const startup = resumirAtividades(lista, false, AGORA);
    expect(startup.atividadesTotal).toBe(5);
    expect(startup.atividadesAprovadas).toBe(1);
    expect(startup.atividadesAtrasadas).toBe(1);
    expect(startup.pendencias).toBe(3);
    expect(startup.proximoPrazo).toBe('2026-11-23');
    expect(resumirAtividades(lista, true, AGORA).pendencias).toBe(1);
  });
  it('sem atividades, tudo zerado', () => {
    expect(resumirAtividades([], false, AGORA)).toEqual({
      atividadesAprovadas: 0, atividadesTotal: 0, atividadesAtrasadas: 0, pendencias: 0, proximoPrazo: null,
    });
  });
});
