// Regras dos arquivos anexados (atividade do mentor e entrega da startup).
// O front confere antes de enviar; o backend precisa conferir de novo (tamanho, extensão e tipo do conteúdo).

export const MAX_ARQUIVOS = 5;
export const MAX_BYTES_ARQUIVO = 10 * 1024 * 1024; // 10 MB cada
export const EXTENSOES_ACEITAS = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'csv', 'png', 'jpg', 'jpeg', 'zip'];
// Para o atributo "accept" do campo de arquivo (o navegador já filtra a janela de escolha)
export const ACCEPT_ARQUIVOS = EXTENSOES_ACEITAS.map((e) => `.${e}`).join(',');

// "1,2 MB", "340 KB", "812 B"
export function formatarTamanho(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined || bytes < 0) return '';
  if (bytes < 1024) return `${bytes} B`;
  const numero = (n: number) => n.toFixed(n >= 10 ? 0 : 1).replace('.', ',');
  if (bytes < 1024 * 1024) return `${numero(bytes / 1024)} KB`;
  return `${numero(bytes / (1024 * 1024))} MB`;
}

export function extensaoDe(nome: string): string {
  const i = nome.lastIndexOf('.');
  return i < 0 ? '' : nome.slice(i + 1).toLowerCase();
}

export interface ResultadoArquivos {
  aceitos: File[];
  // Mensagem para mostrar quando algum arquivo foi recusado (vazia se tudo certo)
  erro: string;
}

// Junta os arquivos novos aos que já estavam escolhidos, recusando os que não servem.
// O mesmo nome e tamanho não entra duas vezes. Devolve a lista final e a mensagem do primeiro problema.
export function juntarArquivos(atuais: File[], novos: File[]): ResultadoArquivos {
  const aceitos = [...atuais];
  let erro = '';
  for (const arquivo of novos) {
    if (!EXTENSOES_ACEITAS.includes(extensaoDe(arquivo.name))) {
      erro ||= `"${arquivo.name}" não é um tipo aceito. Use ${EXTENSOES_ACEITAS.join(', ')}.`;
    } else if (arquivo.size > MAX_BYTES_ARQUIVO) {
      erro ||= `"${arquivo.name}" passa de ${formatarTamanho(MAX_BYTES_ARQUIVO)}.`;
    } else if (arquivo.size === 0) {
      erro ||= `"${arquivo.name}" está vazio.`;
    } else if (aceitos.some((a) => a.name === arquivo.name && a.size === arquivo.size)) {
      continue; // já escolhido
    } else if (aceitos.length >= MAX_ARQUIVOS) {
      erro ||= `Dá para anexar até ${MAX_ARQUIVOS} arquivos.`;
    } else {
      aceitos.push(arquivo);
    }
  }
  return { aceitos, erro };
}
