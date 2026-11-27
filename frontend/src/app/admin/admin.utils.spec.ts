import { Dashboard } from './admin.models';
import { decisoes, iniciais, juntar, largura, ultimosDias, variacao } from './admin.utils';

describe('variacao', () => {
  it('mostra a porcentagem de mudança em relação ao período anterior', () => {
    expect(variacao(9, 7)).toEqual({ texto: '+29%', sentido: 'subiu' });
    expect(variacao(6, 8)).toEqual({ texto: '-25%', sentido: 'caiu' });
    expect(variacao(4, 4)).toEqual({ texto: 'igual', sentido: 'igual' });
  });

  it('quando antes era zero, mostra o número novo (não existe "infinito por cento")', () => {
    expect(variacao(3, 0)).toEqual({ texto: '+3', sentido: 'subiu' });
  });
});

describe('largura', () => {
  it('é proporcional ao maior valor, com um mínimo para a barra não sumir', () => {
    expect(largura(9, 9)).toBe(100);
    expect(largura(3, 9)).toBe(33);
    expect(largura(1, 200)).toBe(2);
    expect(largura(0, 9)).toBe(0);
    expect(largura(5, 0)).toBe(0);
  });
});

describe('ultimosDias', () => {
  it('conta hoje como o último dia do período', () => {
    expect(ultimosDias(30, new Date(2026, 10, 27))).toEqual({ inicio: '2026-10-29', fim: '2026-11-27' });
    expect(ultimosDias(1, new Date(2026, 0, 5))).toEqual({ inicio: '2026-01-05', fim: '2026-01-05' });
  });
});

describe('iniciais e juntar', () => {
  it('iniciais do nome, ou do e-mail quando não há nome', () => {
    expect(iniciais('Feira Já', 'x@y.com')).toBe('FJ');
    expect(iniciais(null, 'admin@ponti.com')).toBe('AP');
  });

  it('junta itens com vírgula e "e"', () => {
    expect(juntar(['Marketing'])).toBe('Marketing');
    expect(juntar(['Marketing', 'Tecnologia', 'Jurídico'])).toBe('Marketing, Tecnologia e Jurídico');
  });
});

describe('decisoes', () => {
  function painel(): Dashboard {
    return {
      periodo: { inicio: '2026-10-29', fim: '2026-11-27', anteriorInicio: '2026-09-29', anteriorFim: '2026-10-28' },
      destaques: [],
      funil: {
        etapas: [
          { etapa: 'CADASTRO', rotulo: 'Cadastro', quantidade: 9, taxaDaAnterior: null, taxaDoInicio: 100, quantidadeAnterior: 7, taxaDaAnteriorNoPeriodoAnterior: null },
          { etapa: 'INTERESSE', rotulo: 'Interesse', quantidade: 6, taxaDaAnterior: 67, taxaDoInicio: 67, quantidadeAnterior: 7, taxaDaAnteriorNoPeriodoAnterior: 100 },
        ],
        maiorQueda: 'INTERESSE',
      },
      match: {
        faixas: [
          { faixa: 'ALTA', rotulo: '70% ou mais', pares: 6, convertidos: 6, taxa: 100, paresAnterior: 1, taxaAnterior: 100 },
          { faixa: 'MEDIA', rotulo: 'De 40% a 69%', pares: 12, convertidos: 9, taxa: 75, paresAnterior: 9, taxaAnterior: 89 },
          { faixa: 'BAIXA', rotulo: 'Menos de 40%', pares: 16, convertidos: 5, taxa: 31, paresAnterior: 12, taxaAnterior: 67 },
        ],
        diferenca: 69,
      },
      ofertaDemanda: [
        { tema: 'MARKETING', rotulo: 'Marketing e marca', startups: 9, mentores: 2, situacao: 'FALTA' },
        { tema: 'JURIDICO', rotulo: 'Jurídico e contratos', startups: 5, mentores: 2, situacao: 'FALTA' },
        { tema: 'GESTAO', rotulo: 'Gestão e time', startups: 0, mentores: 3, situacao: 'SOBRA' },
      ],
      saude: { ativos: 4, entregasNoPrazo: 6, entregasAtrasadas: 1, atividadesVencidas: 3, percentualNoPrazo: 86,
        parados: [{ acompanhamentoId: 6, startup: 'Rota Verde', mentor: 'Ana Paula Souza', diasSemMovimento: 16 }] },
      atencao: [],
    };
  }

  it('cada bloco ganha uma frase simples e, quando há, o que fazer', () => {
    const t = decisoes(painel());
    expect(t.funil).toEqual({
      texto: '3 startups se cadastraram e não pediram nenhuma mentoria.',
      acao: 'mostrar os mentores recomendados logo depois do cadastro.',
    });
    expect(t.match.texto).toBe('Com compatibilidade alta, 100% dos pedidos foram aceitos. Com compatibilidade baixa, só 31%. A recomendação da Ponti funciona.');
    expect(t.match.acao).toBeNull();
    expect(t.oferta).toEqual({
      texto: 'Faltam mentores em Marketing e marca e Jurídico e contratos.',
      acao: 'priorizar esses temas ao recrutar mentores.',
    });
    expect(t.saude).toEqual({ texto: '1 acompanhamento está sem movimento há mais de 14 dias.', acao: 'falar com o mentor.' });
    expect(t.atencao.texto).toBe('Nenhuma conta precisa de atenção agora.');
  });

  it('com 1 startup parada, a frase fica no singular', () => {
    const d = painel();
    d.funil.etapas[1].quantidade = 8;
    expect(decisoes(d).funil.texto).toBe('1 startup se cadastrou e não pediu nenhuma mentoria.');
  });

  it('match com pouca diferença entre as faixas pede revisão dos pesos', () => {
    const d = painel();
    d.match.diferenca = 10;
    expect(decisoes(d).match.acao).toBe('revisar os pesos do cálculo de compatibilidade.');
  });

  it('sem dados, explica que ainda não dá para concluir', () => {
    const d = painel();
    d.funil.maiorQueda = null;
    d.match.diferenca = null;
    expect(decisoes(d).funil).toEqual({ texto: 'Nenhuma startup se cadastrou neste período.', acao: null });
    expect(decisoes(d).match.texto).toContain('Ainda não há pedidos suficientes');
  });
});
