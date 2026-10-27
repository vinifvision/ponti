// Datas da agenda. O backend manda data e hora sem fuso ("2026-11-03T09:00:00", horário de Recife),
// então tudo aqui trabalha com os números do texto, sem converter fuso.

const MESES = [
  'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
];
const DIAS = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado'];

// "2026-11-03T09:00:00" -> Date local com os mesmos números
export function lerDataHora(texto: string): Date {
  const [data, hora = '00:00'] = texto.split('T');
  const [ano, mes, dia] = data.split('-').map(Number);
  const [h, m] = hora.split(':').map(Number);
  return new Date(ano, mes - 1, dia, h, m);
}

// Date -> "2026-11-03"
export function dataIso(d: Date): string {
  return `${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())}`;
}

// Date -> "2026-11-03T09:00:00"
export function dataHoraIso(d: Date): string {
  return `${dataIso(d)}T${dois(d.getHours())}:${dois(d.getMinutes())}:00`;
}

// "09:00"
export function hora(texto: string): string {
  const d = lerDataHora(texto);
  return `${dois(d.getHours())}:${dois(d.getMinutes())}`;
}

// "14 de novembro de 2026"
export function dataPorExtenso(texto: string): string {
  const d = lerDataHora(texto);
  return `${d.getDate()} de ${MESES[d.getMonth()]} de ${d.getFullYear()}`;
}

// "terça-feira, 3 de novembro, às 09:00"
export function dataHoraCompleta(texto: string): string {
  const d = lerDataHora(texto);
  return `${DIAS[d.getDay()]}, ${d.getDate()} de ${MESES[d.getMonth()]}, às ${hora(texto)}`;
}

export function nomeDoMes(mes: number): string {
  const nome = MESES[mes];
  return nome.charAt(0).toUpperCase() + nome.slice(1);
}

export function jaPassou(texto: string): boolean {
  return lerDataHora(texto).getTime() <= Date.now();
}

function dois(n: number): string {
  return String(n).padStart(2, '0');
}
