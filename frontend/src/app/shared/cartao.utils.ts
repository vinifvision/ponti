// Ajudas do formulário de cartão (o backend valida de novo; aqui é só para a pessoa digitar melhor).

export function somenteDigitos(texto: string): string {
  return (texto || '').replace(/\D/g, '');
}

// "4242424242424242" -> "4242 4242 4242 4242"
export function formatarNumeroCartao(texto: string): string {
  return somenteDigitos(texto).slice(0, 19).replace(/(\d{4})(?=\d)/g, '$1 ');
}

// "1235" -> "12/35"
export function formatarValidade(texto: string): string {
  const d = somenteDigitos(texto).slice(0, 4);
  return d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
}

export function bandeiraDoCartao(texto: string): string {
  const n = somenteDigitos(texto);
  if (/^(4011|4312|4389|4514|4576|5041|5066|5067|509|6277|6362|6363|650|6516|6550)/.test(n)) return 'Elo';
  if (/^4/.test(n)) return 'Visa';
  if (/^(5[1-5]|2[2-7])/.test(n)) return 'Mastercard';
  if (/^3[47]/.test(n)) return 'American Express';
  if (/^(606282|3841)/.test(n)) return 'Hipercard';
  return '';
}

// Algoritmo de Luhn: o mesmo dígito verificador que os bancos usam
export function numeroCartaoValido(texto: string): boolean {
  const n = somenteDigitos(texto);
  if (n.length < 13 || n.length > 19) return false;
  let soma = 0;
  let dobrar = false;
  for (let i = n.length - 1; i >= 0; i--) {
    let d = Number(n[i]);
    if (dobrar) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    soma += d;
    dobrar = !dobrar;
  }
  return soma % 10 === 0;
}
