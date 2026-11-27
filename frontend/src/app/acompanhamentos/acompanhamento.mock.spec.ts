import { BancoMock, ErroMock, TipoMock } from './acompanhamento.mock';

const AGORA = new Date(2026, 10, 20, 12, 0);

function banco(tipo: TipoMock) {
  return new BancoMock(tipo, tipo === 'MENTOR' ? 'Juliana G.' : 'NutriPet', () => AGORA);
}

// Código HTTP do erro lançado (ou null se não deu erro)
function status(acao: () => unknown): number | null {
  try {
    acao();
    return null;
  } catch (e) {
    return e instanceof ErroMock ? e.status : -1;
  }
}

function idPrincipal(b: BancoMock) {
  return b.listar().filter((a) => a.status === 'ATIVO')[0].id;
}

function atividadeCom(b: BancoMock, estado: string) {
  const a = b.atividades(idPrincipal(b)).find((x) => x.status === estado);
  if (!a) throw new Error('atividade de exemplo não encontrada: ' + estado);
  return a;
}

describe('BancoMock: lista e painel', () => {
  it('a startup vê 4 acompanhamentos, com números calculados das atividades', () => {
    const lista = banco('STARTUP').listar();
    expect(lista.map((a) => a.status)).toEqual(['PROPOSTO', 'ATIVO', 'ATIVO', 'ENCERRADO']);
    const principal = lista[1];
    expect(principal.atividadesTotal).toBe(5);
    expect(principal.atividadesAprovadas).toBe(1);
    expect(principal.atividadesAtrasadas).toBe(2);
    expect(principal.pendencias).toBe(3);
  });
  it('o mentor vê as startups e conta o que precisa revisar', () => {
    const lista = banco('MENTOR').listar();
    expect(lista[1].parceiro.nome).toBe('VetTech');
    expect(lista[1].pendencias).toBe(1);
  });
  it('o painel soma só os acompanhamentos ativos', () => {
    const painel = banco('STARTUP').painel();
    expect(painel.acompanhamentosAtivos).toBe(2);
    expect(painel.atividadesTotal).toBe(8);
    expect(painel.atividadesAprovadas).toBe(2);
  });
  it('acompanhamento inexistente dá 404', () => {
    expect(status(() => banco('STARTUP').detalhe(999))).toBe(404);
  });
});

describe('BancoMock: entregar, aprovar e devolver', () => {
  it('a startup entrega uma atividade pendente e ela vai para revisão', () => {
    const b = banco('STARTUP');
    const pendente = atividadeCom(b, 'PENDENTE');
    const depois = b.entregar(pendente.id, { texto: 'Pronto!', link: null });
    expect(depois.status).toBe('ENTREGUE');
    expect(depois.entregas.length).toBe(1);
    expect(depois.entregas[0].texto).toBe('Pronto!');
  });
  it('não aceita entrega vazia, de mentor ou de atividade já aprovada', () => {
    const b = banco('STARTUP');
    expect(status(() => b.entregar(atividadeCom(b, 'PENDENTE').id, { texto: '  ', link: null }))).toBe(400);
    expect(status(() => b.entregar(atividadeCom(b, 'APROVADA').id, { texto: 'x', link: null }))).toBe(409);
    const m = banco('MENTOR');
    expect(status(() => m.entregar(atividadeCom(m, 'PENDENTE').id, { texto: 'x', link: null }))).toBe(403);
  });
  it('o mentor aprova com ou sem feedback', () => {
    const m = banco('MENTOR');
    const aprovada = m.aprovar(atividadeCom(m, 'ENTREGUE').id, '  Muito bom  ');
    expect(aprovada.status).toBe('APROVADA');
    expect(aprovada.entregas[aprovada.entregas.length - 1].feedback).toBe('Muito bom');
  });
  it('devolver exige feedback; depois a startup reenvia e o histórico guarda as duas entregas', () => {
    const m = banco('MENTOR');
    const entregue = atividadeCom(m, 'ENTREGUE');
    expect(status(() => m.devolver(entregue.id, '   '))).toBe(400);
    expect(m.devolver(entregue.id, 'Ajuste o segmento').status).toBe('DEVOLVIDA');

    const s = banco('STARTUP');
    const devolvida = atividadeCom(s, 'DEVOLVIDA');
    const reenviada = s.entregar(devolvida.id, { texto: 'Versão 2', link: null });
    expect(reenviada.status).toBe('ENTREGUE');
    expect(reenviada.entregas.length).toBe(2);
    expect(reenviada.entregas[0].feedback).toBe('Faltou separar os ganhos por segmento de cliente. Revise e reenvie.');
  });
  it('a startup não aprova e o mentor não avalia o que não foi entregue', () => {
    const s = banco('STARTUP');
    expect(status(() => s.aprovar(atividadeCom(s, 'ENTREGUE').id))).toBe(403);
    const m = banco('MENTOR');
    expect(status(() => m.aprovar(atividadeCom(m, 'PENDENTE').id))).toBe(409);
  });
});

describe('BancoMock: comentários e atividades novas', () => {
  it('os dois lados comentam na entrega, identificados por papel', () => {
    const s = banco('STARTUP');
    const entregue = atividadeCom(s, 'ENTREGUE');
    const atual = s.comentar(entregue.id, entregue.entregas[0].id, '  Obrigada!  ');
    const ultimo = atual.entregas[0].comentarios[atual.entregas[0].comentarios.length - 1];
    expect(ultimo.texto).toBe('Obrigada!');
    expect(ultimo.autor).toBe('STARTUP');
    expect(ultimo.nomeAutor).toBe('NutriPet');

    const m = banco('MENTOR');
    const e = atividadeCom(m, 'ENTREGUE');
    const c = m.comentar(e.id, e.entregas[0].id, 'Vamos conversar').entregas[0].comentarios;
    expect(c[c.length - 1].autor).toBe('MENTOR');
    expect(status(() => m.comentar(e.id, e.entregas[0].id, ' '))).toBe(400);
  });
  it('acompanhamento encerrado é somente leitura (409)', () => {
    const m = banco('MENTOR');
    const encerrado = m.listar().find((a) => a.status === 'ENCERRADO')!;
    const atividade = m.atividades(encerrado.id)[0];
    expect(status(() => m.comentar(atividade.id, atividade.entregas[0].id, 'oi'))).toBe(409);
    expect(status(() => m.criarAtividade(encerrado.id, { titulo: 'Nova', descricao: '', prazo: '2026-12-01T23:59:00' }))).toBe(409);
  });
  it('só o mentor cria atividade, e ela entra pendente e na ordem dos prazos', () => {
    const m = banco('MENTOR');
    const id = idPrincipal(m);
    const nova = m.criarAtividade(id, { titulo: '  Nova  ', descricao: 'Detalhes', prazo: '2026-11-21T23:59:00' });
    expect(nova.titulo).toBe('Nova');
    expect(nova.status).toBe('PENDENTE');
    const ordem = m.atividades(id).map((a) => a.prazo);
    expect([...ordem].sort()).toEqual(ordem);
    expect(m.atividades(id).length).toBe(6);
    expect(status(() => m.criarAtividade(id, { titulo: ' ', descricao: '', prazo: '2026-12-01T23:59:00' }))).toBe(400);
    const s = banco('STARTUP');
    expect(status(() => s.criarAtividade(idPrincipal(s), { titulo: 'x', descricao: '', prazo: '2026-12-01T23:59:00' }))).toBe(403);
  });
});

describe('BancoMock: propostas', () => {
  it('o mentor propõe e a proposta entra na lista com as metas válidas contadas', () => {
    const m = banco('MENTOR');
    const nova = m.propor({ startupId: 77, objetivo: ' Validar o MVP ', metas: ['Meta A', '  ', 'Meta B'], nomeStartup: 'Startup X' });
    expect(nova.status).toBe('PROPOSTO');
    expect(nova.objetivo).toBe('Validar o MVP');
    expect(nova.metasSugeridas).toBe(2);
    expect(nova.parceiro.nome).toBe('Startup X');
    expect(m.listar()[0].id).toBe(nova.id);
  });
  it('no máximo uma proposta ou acompanhamento ativo por startup (409) e só mentor propõe (403)', () => {
    const m = banco('MENTOR');
    expect(status(() => m.propor({ startupId: 1, objetivo: 'x', metas: [] }))).toBe(409); // NutriPet já tem proposta
    expect(status(() => m.propor({ startupId: 2, objetivo: 'x', metas: [] }))).toBe(409); // VetTech já está ativa
    expect(status(() => m.propor({ startupId: 4, objetivo: 'x', metas: [] }))).toBe(null); // ClinApp só tem encerrado
    const s = banco('STARTUP');
    expect(status(() => s.propor({ startupId: 9, objetivo: 'x', metas: [] }))).toBe(403);
  });
  it('a startup aceita (vira ativo, sem atividades) ou recusa (some da lista)', () => {
    const s = banco('STARTUP');
    const proposta = s.listar()[0];
    s.aceitar(proposta.id);
    const ativo = s.listar().find((a) => a.id === proposta.id)!;
    expect(ativo.status).toBe('ATIVO');
    expect(ativo.metasTotal).toBe(3);
    expect(ativo.atividadesTotal).toBe(0);
    expect(status(() => s.aceitar(proposta.id))).toBe(409);

    const s2 = banco('STARTUP');
    const id = s2.listar()[0].id;
    s2.recusar(id);
    expect(s2.listar().some((a) => a.id === id)).toBe(false);
    const m = banco('MENTOR');
    expect(status(() => m.aceitar(m.listar()[0].id))).toBe(403);
  });
});

describe('BancoMock: arquivos anexados e desfazer envio', () => {
  const arquivo = (nome: string, conteudo = 'conteudo') => new File([conteudo], nome, { type: 'application/pdf' });

  it('o mentor cria a atividade com arquivos e eles viram anexos', () => {
    const b = banco('MENTOR');
    const nova = b.criarAtividade(idPrincipal(b), {
      titulo: 'Montar o roteiro',
      descricao: '',
      prazo: '2026-12-10T23:59:00',
      arquivos: [arquivo('Modelo.pdf', 'abc'), arquivo('Guia.pdf')],
    });
    expect(nova.anexos?.map((a) => a.nome)).toEqual(['Modelo.pdf', 'Guia.pdf']);
    expect(nova.anexos?.[0].tamanho).toBe(3);
    expect(nova.anexos?.[0].url).toContain('blob:');
  });

  it('atividade criada sem arquivos fica com a lista de anexos vazia', () => {
    const b = banco('MENTOR');
    const nova = b.criarAtividade(idPrincipal(b), { titulo: 'Sem anexos', descricao: '', prazo: '2026-12-10T23:59:00' });
    expect(nova.anexos).toEqual([]);
  });

  it('a startup pode entregar só com um arquivo, sem texto nem link', () => {
    const b = banco('STARTUP');
    const pendente = atividadeCom(b, 'PENDENTE');
    const atualizada = b.entregar(pendente.id, { texto: '', link: null, arquivos: [arquivo('Pesquisa.pdf')] });
    expect(atualizada.status).toBe('ENTREGUE');
    expect(atualizada.entregas[atualizada.entregas.length - 1].anexos?.[0].nome).toBe('Pesquisa.pdf');
  });

  it('entregar sem texto, link nem arquivo continua dando erro 400', () => {
    const b = banco('STARTUP');
    const pendente = atividadeCom(b, 'PENDENTE');
    expect(status(() => b.entregar(pendente.id, { texto: ' ', link: null, arquivos: [] }))).toBe(400);
  });

  it('desfazer o envio de uma primeira entrega volta a atividade para pendente', () => {
    const b = banco('STARTUP');
    const entregue = atividadeCom(b, 'ENTREGUE');
    const antes = entregue.entregas.length;
    const depois = b.desfazerEntrega(entregue.id);
    expect(depois.status).toBe('PENDENTE');
    expect(depois.entregas.length).toBe(antes - 1);
  });

  it('desfazer um reenvio volta para devolvida e mantém a entrega anterior', () => {
    const b = banco('STARTUP');
    const devolvida = atividadeCom(b, 'DEVOLVIDA');
    const reenviada = b.entregar(devolvida.id, { texto: 'Nova versão', link: null });
    expect(reenviada.entregas.length).toBe(devolvida.entregas.length + 1);
    const depois = b.desfazerEntrega(devolvida.id);
    expect(depois.status).toBe('DEVOLVIDA');
    expect(depois.entregas.length).toBe(devolvida.entregas.length);
  });

  it('só dá para desfazer enquanto a atividade está entregue, e só a startup faz isso', () => {
    const b = banco('STARTUP');
    expect(status(() => b.desfazerEntrega(atividadeCom(b, 'PENDENTE').id))).toBe(409);
    expect(status(() => b.desfazerEntrega(atividadeCom(b, 'APROVADA').id))).toBe(409);
    const m = banco('MENTOR');
    expect(status(() => m.desfazerEntrega(atividadeCom(m, 'ENTREGUE').id))).toBe(403);
  });
});
