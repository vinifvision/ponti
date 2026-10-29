import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SeletorTemasComponent } from './seletor-temas.component';

describe('SeletorTemasComponent', () => {
  let fixture: ComponentFixture<SeletorTemasComponent>;
  let http: HttpTestingController;

  const botoes = () => Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button[aria-pressed]'));

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SeletorTemasComponent);
    fixture.componentRef.setInput('max', 2);
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/temas')).flush([
      { codigo: 'VALIDACAO', nome: 'Validação de ideia' },
      { codigo: 'PITCH', nome: 'Pitch e captação' },
      { codigo: 'VENDAS', nome: 'Vendas' },
    ]);
    fixture.detectChanges();
  });

  it('mostra um botão por tema', () => {
    expect(botoes().map((b) => b.textContent!.trim())).toEqual(['Validação de ideia', 'Pitch e captação', 'Vendas']);
  });

  it('marca e desmarca ao clicar', () => {
    botoes()[0].click();
    fixture.detectChanges();
    expect(fixture.componentInstance.selecionados()).toEqual(['VALIDACAO']);
    botoes()[0].click();
    fixture.detectChanges();
    expect(fixture.componentInstance.selecionados()).toEqual([]);
  });

  it('não deixa passar do limite e desabilita os outros', () => {
    botoes()[0].click();
    botoes()[1].click();
    fixture.detectChanges();
    expect(botoes()[2].disabled).toBeTrue();
    botoes()[2].click();
    expect(fixture.componentInstance.selecionados()).toEqual(['VALIDACAO', 'PITCH']);
  });
});
