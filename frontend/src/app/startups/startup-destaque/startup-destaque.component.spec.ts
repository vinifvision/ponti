import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Startup } from '../../models';
import { StartupDestaqueComponent } from './startup-destaque.component';

describe('StartupDestaqueComponent', () => {
  let fixture: ComponentFixture<StartupDestaqueComponent>;

  const base: Startup = { id: 7, nome: 'PetCare Digital', compatibilidade: 80, motivos: ['Precisa de validação'] };

  function abrir(startup: Startup, situacao: string | null = null) {
    fixture.componentRef.setInput('startup', startup);
    fixture.componentRef.setInput('situacao', situacao);
    fixture.detectChanges();
  }
  const el = (): HTMLElement => fixture.nativeElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    fixture = TestBed.createComponent(StartupDestaqueComponent);
  });

  it('mostra a miniatura do vídeo de pitch quando há link do YouTube', () => {
    abrir({ ...base, videoPitchUrl: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ' });
    const img = el().querySelector('img')!;
    expect(img.getAttribute('src')).toBe('https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg');
    expect(el().querySelector('a[aria-label^="Assistir ao pitch"]')).not.toBeNull();
  });

  it('cai para a miniatura menor quando a de alta resolução não existe', () => {
    abrir({ ...base, videoPitchUrl: 'https://youtu.be/dQw4w9WgXcQ' });
    el().querySelector('img')!.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(el().querySelector('img')!.getAttribute('src')).toBe('https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg');
  });

  it('sem vídeo mostra o logo; sem logo, as iniciais', () => {
    abrir({ ...base, fotoUrl: 'https://exemplo.com/logo.png' });
    expect(el().querySelector('img')!.getAttribute('alt')).toBe('Logo de PetCare Digital');

    fixture = TestBed.createComponent(StartupDestaqueComponent);
    abrir(base);
    expect(el().querySelector('img')).toBeNull();
    expect(el().textContent).toContain('PD');
  });

  it('o botão sugere mentoria, e fica desabilitado quando já existe sugestão', () => {
    let emitida: Startup | undefined;
    fixture.componentInstance.convidar.subscribe((s) => (emitida = s));
    abrir(base);
    const botao = el().querySelector('button')!;
    expect(botao.textContent!.trim()).toBe('Sugerir mentoria');
    botao.click();
    expect(emitida).toEqual(base);

    fixture = TestBed.createComponent(StartupDestaqueComponent);
    abrir(base, 'Sugestão enviada');
    const enviado = el().querySelector('button')!;
    expect(enviado.textContent!.trim()).toBe('Sugestão enviada');
    expect(enviado.disabled).toBeTrue();
  });
});
