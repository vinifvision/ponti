// Textos e conversões usados nos cards e no perfil do mentor.

const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

// 0 vira "Gratuita"; vazio vira "A combinar"
export function formatarValorSessao(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) return 'A combinar';
  if (Number(valor) === 0) return 'Gratuita';
  return MOEDA.format(Number(valor));
}

export function formatarDuracao(minutos: number | null | undefined): string {
  if (!minutos) return '';
  if (minutos % 60 === 0) return minutos === 60 ? '1 hora' : `${minutos / 60} horas`;
  return `${minutos} min`;
}

// Período de uma certificação: "2025 - 2026", "2026 - atual" ou só "2024"
export function formatarPeriodo(inicio?: number | null, fim?: number | null): string {
  if (inicio && fim) return inicio === fim ? `${inicio}` : `${inicio} - ${fim}`;
  if (inicio) return `${inicio} - atual`;
  if (fim) return `${fim}`;
  return '';
}

// Tira o id de 11 caracteres de um link do YouTube (watch, youtu.be, shorts ou embed).
// Só esse id entra no endereço do player, então nenhum link estranho vira <iframe>.
export function idVideoYoutube(url: string | null | undefined): string | null {
  if (!url) return null;
  const achado = url
    .trim()
    .match(/(?:youtube\.com\/(?:watch\?(?:.*&)?v=|embed\/|shorts\/)|youtu\.be\/)([A-Za-z0-9_-]{11})/);
  return achado ? achado[1] : null;
}

// Garante que o link abre um site (http/https), para não virar link "javascript:"
export function linkSeguro(url: string | null | undefined): string | null {
  if (!url) return null;
  const limpo = url.trim();
  if (/^https?:\/\//i.test(limpo)) return limpo;
  if (/^[\w.-]+\.[a-z]{2,}(\/.*)?$/i.test(limpo)) return `https://${limpo}`;
  return null;
}

// Link do WhatsApp a partir do número (só os dígitos)
export function linkWhatsapp(numero: string | null | undefined): string | null {
  const digitos = (numero ?? '').replace(/\D/g, '');
  return digitos.length >= 10 ? `https://wa.me/${digitos}` : null;
}
