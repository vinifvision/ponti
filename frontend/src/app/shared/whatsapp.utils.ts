// O backend guarda o WhatsApp como 55 + DDD + número, só dígitos (ex.: "5581999998888").

// "5581999998888" -> "(81) 99999-8888"; "558133334444" -> "(81) 3333-4444"
export function formatarWhatsapp(numero: string | null | undefined): string {
  if (!numero) return '';
  const local = numero.startsWith('55') ? numero.slice(2) : numero;
  const m = /^(\d{2})(\d{4,5})(\d{4})$/.exec(local);
  return m ? `(${m[1]}) ${m[2]}-${m[3]}` : numero;
}

// Link que abre a conversa no WhatsApp (app ou web). Só aceita o formato do backend.
export function linkWhatsapp(numero: string | null | undefined): string | null {
  return numero && /^55\d{10,11}$/.test(numero) ? `https://wa.me/${numero}` : null;
}
