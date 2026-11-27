import { Agendamento } from '../models';
import { ResumoAcompanhamento } from './acompanhamento.models';
import { startupsElegiveis } from './elegibilidade.utils';

function ag(id: number, startupId: number, nome: string, inicio: string, realizada = true): Agendamento {
  return {
    id, inicio, fim: inicio, valor: 100, status: 'CONFIRMADO', statusPagamento: 'PAGO',
    startup: { id: startupId, nome, descricao: 'PetTech' }, mentor: { id: 1, nome: 'Mentor' }, realizada,
  } as Agendamento;
}

function ac(startupId: number, status: ResumoAcompanhamento['status']): ResumoAcompanhamento {
  return {
    id: startupId * 10, status, objetivo: 'x', dataInicio: null, dataFim: null,
    parceiro: { id: startupId, nome: 'x', fotoUrl: null, subtitulo: null },
    atividadesAprovadas: 0, atividadesTotal: 0, metasCumpridas: 0, metasTotal: 0,
    proximoPrazo: null, atividadesAtrasadas: 0, pendencias: 0,
  };
}

describe('startupsElegiveis', () => {
  it('só entram startups com mentoria realizada', () => {
    const r = startupsElegiveis([ag(1, 5, 'A', '2026-10-01T10:00:00'), ag(2, 6, 'B', '2026-10-02T10:00:00', false)], []);
    expect(r.map((s) => s.nome)).toEqual(['A']);
  });

  it('conta as mentorias e guarda a mais recente; a startup mais recente vem primeiro', () => {
    const r = startupsElegiveis(
      [ag(1, 5, 'A', '2026-09-01T10:00:00'), ag(2, 5, 'A', '2026-10-20T10:00:00'), ag(3, 6, 'B', '2026-10-25T10:00:00')],
      [],
    );
    expect(r.map((s) => s.nome)).toEqual(['B', 'A']);
    expect(r[1].mentoriasRealizadas).toBe(2);
    expect(r[1].ultimaMentoria).toBe('2026-10-20T10:00:00');
  });

  it('fica de fora quem já tem proposta pendente ou acompanhamento ativo', () => {
    const ags = [ag(1, 5, 'A', '2026-10-01T10:00:00'), ag(2, 6, 'B', '2026-10-02T10:00:00'), ag(3, 7, 'C', '2026-10-03T10:00:00')];
    const r = startupsElegiveis(ags, [ac(5, 'PROPOSTO'), ac(6, 'ATIVO')]);
    expect(r.map((s) => s.nome)).toEqual(['C']);
  });

  it('acompanhamento encerrado não impede uma nova proposta', () => {
    const r = startupsElegiveis([ag(1, 5, 'A', '2026-10-01T10:00:00')], [ac(5, 'ENCERRADO')]);
    expect(r.map((s) => s.nome)).toEqual(['A']);
  });

  it('ignora agendamento sem startup', () => {
    const semStartup = { ...ag(1, 5, 'A', '2026-10-01T10:00:00'), startup: null } as Agendamento;
    expect(startupsElegiveis([semStartup], [])).toEqual([]);
  });
});
