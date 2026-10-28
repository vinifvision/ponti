import { contarPedidosPendentes, contarSugestoesPendentes, descreverPendencias } from './pendencias';

describe('pendências do sino', () => {
  it('mentor: conta só os pedidos de mentoria ainda pendentes', () => {
    const lista = [
      { status: 'PENDENTE' },
      { status: 'CONFIRMADO' },
      { status: 'PENDENTE' },
      { status: 'RECUSADO' },
      { status: 'CANCELADO' },
    ] as const;
    expect(contarPedidosPendentes([...lista])).toBe(2);
    expect(contarPedidosPendentes([])).toBe(0);
  });

  it('startup: conta só as sugestões de mentor ainda pendentes', () => {
    const convites = [
      { enviadoPor: 'MENTOR', status: 'PENDENTE' },
      { enviadoPor: 'MENTOR', status: 'ACEITO' },
      { enviadoPor: 'STARTUP', status: 'PENDENTE' }, // foi a própria startup que enviou
      { enviadoPor: 'MENTOR', status: 'RECUSADO' },
      { enviadoPor: 'MENTOR', status: 'PENDENTE' },
    ] as const;
    expect(contarSugestoesPendentes([...convites])).toBe(2);
  });

  it('descreve a quantidade para leitores de tela', () => {
    expect(descreverPendencias(0)).toBe('Notificações: nada pendente');
    expect(descreverPendencias(1)).toBe('Notificações: 1 pendência');
    expect(descreverPendencias(3)).toBe('Notificações: 3 pendências');
  });
});
