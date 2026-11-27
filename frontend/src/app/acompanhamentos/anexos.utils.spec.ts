import { MAX_ARQUIVOS, MAX_BYTES_ARQUIVO, extensaoDe, formatarTamanho, juntarArquivos } from './anexos.utils';

const arquivo = (nome: string, tamanho = 10) => new File([new Uint8Array(tamanho)], nome);

describe('formatarTamanho', () => {
  it('mostra bytes, KB e MB com vírgula', () => {
    expect(formatarTamanho(812)).toBe('812 B');
    expect(formatarTamanho(2048)).toBe('2,0 KB');
    expect(formatarTamanho(340 * 1024)).toBe('340 KB');
    expect(formatarTamanho(1.2 * 1024 * 1024)).toBe('1,2 MB');
  });
  it('devolve vazio quando não há tamanho', () => {
    expect(formatarTamanho(null)).toBe('');
    expect(formatarTamanho(undefined)).toBe('');
  });
});

describe('extensaoDe', () => {
  it('pega a última extensão em minúsculas', () => {
    expect(extensaoDe('Relatório.Final.PDF')).toBe('pdf');
    expect(extensaoDe('semextensao')).toBe('');
  });
});

describe('juntarArquivos', () => {
  it('aceita tipos permitidos e soma aos já escolhidos', () => {
    const r = juntarArquivos([arquivo('a.pdf')], [arquivo('b.docx'), arquivo('c.png')]);
    expect(r.aceitos.map((a) => a.name)).toEqual(['a.pdf', 'b.docx', 'c.png']);
    expect(r.erro).toBe('');
  });
  it('recusa tipo não permitido, mas mantém os bons', () => {
    const r = juntarArquivos([], [arquivo('virus.exe'), arquivo('ok.pdf')]);
    expect(r.aceitos.map((a) => a.name)).toEqual(['ok.pdf']);
    expect(r.erro).toContain('virus.exe');
  });
  it('recusa arquivo grande demais e arquivo vazio', () => {
    const grande = { name: 'grande.pdf', size: MAX_BYTES_ARQUIVO + 1 } as File;
    expect(juntarArquivos([], [grande]).aceitos).toEqual([]);
    expect(juntarArquivos([], [grande]).erro).toContain('grande.pdf');
    expect(juntarArquivos([], [arquivo('vazio.pdf', 0)]).erro).toContain('vazio.pdf');
  });
  it('não repete o mesmo arquivo (mesmo nome e tamanho)', () => {
    const r = juntarArquivos([arquivo('a.pdf', 5)], [arquivo('a.pdf', 5)]);
    expect(r.aceitos.length).toBe(1);
    expect(r.erro).toBe('');
  });
  it('respeita o limite de arquivos', () => {
    const atuais = Array.from({ length: MAX_ARQUIVOS }, (_, i) => arquivo(`f${i}.pdf`, i + 1));
    const r = juntarArquivos(atuais, [arquivo('extra.pdf', 99)]);
    expect(r.aceitos.length).toBe(MAX_ARQUIVOS);
    expect(r.erro).toContain(String(MAX_ARQUIVOS));
  });
});
