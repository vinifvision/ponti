import { formatarWhatsapp, linkWhatsapp } from './whatsapp.utils';

describe('whatsapp.utils', () => {
  it('formata celular e fixo no padrão brasileiro', () => {
    expect(formatarWhatsapp('5581999998888')).toBe('(81) 99999-8888');
    expect(formatarWhatsapp('558133334444')).toBe('(81) 3333-4444');
    expect(formatarWhatsapp(null)).toBe('');
  });

  it('monta o link wa.me só para número válido', () => {
    expect(linkWhatsapp('5581999998888')).toBe('https://wa.me/5581999998888');
    expect(linkWhatsapp('javascript:alert(1)')).toBeNull();
    expect(linkWhatsapp(undefined)).toBeNull();
  });
});
