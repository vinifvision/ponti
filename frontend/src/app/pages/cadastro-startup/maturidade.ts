// Questionário de maturidade da startup, baseado nos níveis de TRL (Technology Readiness Level) da NASA.
// Os níveis 7 a 9 foram adaptados de "ambiente espacial / voo / missão" para "ambiente real / operação / produção".
// O nível 1 ("Ideia inicial") é o ponto de partida de toda startup, por isso o questionário começa no nível 2.

export interface PerguntaMaturidade {
  /** Nível que a startup alcança se responder "Sim" a esta pergunta (2 a 9). */
  nivel: number;
  titulo: string;
  pergunta: string;
  ajuda: string;
}

export const NIVEL_INICIAL = 1;
export const NIVEL_MAXIMO = 9;

export const NOMES_NIVEIS: Record<number, string> = {
  1: 'Ideia inicial',
  2: 'Conceito formulado',
  3: 'Pesquisa e design ativos',
  4: 'Prova de conceito pronta',
  5: 'Versão básica testada',
  6: 'Protótipo funcional',
  7: 'Demonstrado no ambiente real',
  8: 'Testada e qualificada',
  9: 'Comprovada em operação',
};

export const PERGUNTAS_MATURIDADE: PerguntaMaturidade[] = [
  {
    nivel: 2,
    titulo: NOMES_NIVEIS[2],
    pergunta:
      'Sua startup já definiu o conceito da solução, mesmo que ainda seja uma aposta, sem comprovação por experimento?',
    ajuda: 'existe uma descrição clara de como a solução funcionaria, mesmo sem teste.',
  },
  {
    nivel: 3,
    titulo: NOMES_NIVEIS[3],
    pergunta:
      'Sua startup está desenvolvendo ativamente a solução (pesquisa e design) e já começou a construir um modelo de prova de conceito?',
    ajuda: 'o design está em andamento e o primeiro modelo está sendo construído.',
  },
  {
    nivel: 4,
    titulo: NOMES_NIVEIS[4],
    pergunta: 'A prova de conceito da sua startup está pronta?',
    ajuda: 'o modelo foi concluído e demonstra que a ideia funciona.',
  },
  {
    nivel: 5,
    titulo: NOMES_NIVEIS[5],
    pergunta:
      'Sua startup montou uma versão básica da solução (breadboard) e a submeteu a testes mais rigorosos do que os da prova de conceito?',
    ajuda: 'a versão básica está montada e passou por testes mais exigentes que os do nível anterior.',
  },
  {
    nivel: 6,
    titulo: NOMES_NIVEIS[6],
    pergunta: 'Sua startup tem um protótipo totalmente funcional, ou um modelo representativo da solução final?',
    ajuda: 'o protótipo completo funciona.',
  },
  {
    nivel: 7,
    titulo: NOMES_NIVEIS[7],
    pergunta: 'Sua startup já demonstrou o protótipo funcionando no ambiente real em que a solução será usada?',
    ajuda: 'o protótipo já rodou na prática, por exemplo num piloto com cliente.',
  },
  {
    nivel: 8,
    titulo: NOMES_NIVEIS[8],
    pergunta:
      'A solução da sua startup foi testada e qualificada, e está pronta para ser integrada ao sistema ou processo de quem vai usá-la?',
    ajuda: 'os testes finais terminaram e a solução está liberada para entrar em operação.',
  },
  {
    nivel: 9,
    titulo: NOMES_NIVEIS[9],
    pergunta: 'A solução da sua startup já foi comprovada em operação real, com resultado bem-sucedido?',
    ajuda: 'a solução está em uso real e funcionou com sucesso.',
  },
];

/**
 * Calcula o nível a partir das respostas, na ordem em que foram dadas.
 * O nível é o da última resposta "Sim" consecutiva (o primeiro "Não" encerra o questionário),
 * com mínimo 1 ("Ideia inicial") e máximo 9.
 */
export function calcularNivel(respostas: boolean[]): number {
  let nivel = NIVEL_INICIAL;
  for (let i = 0; i < respostas.length && i < PERGUNTAS_MATURIDADE.length; i++) {
    if (!respostas[i]) break;
    nivel = PERGUNTAS_MATURIDADE[i].nivel;
  }
  return nivel;
}
