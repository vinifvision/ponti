import { TestBed } from '@angular/core/testing';
import { CompatibilidadeComponent } from './compatibilidade.component';

describe('CompatibilidadeComponent', () => {
  function montar(pontos: number | null, motivos: string[] = []) {
    const fixture = TestBed.createComponent(CompatibilidadeComponent);
    fixture.componentRef.setInput('pontos', pontos);
    fixture.componentRef.setInput('motivos', motivos);
    fixture.detectChanges();
    return fixture;
  }

  it('mostra a porcentagem, o nível e os motivos', () => {
    const f = montar(75, ['Ajuda com Pitch e captação', 'Conhece o setor PetTech']);
    const texto = (f.nativeElement as HTMLElement).textContent!;
    expect(texto).toContain('75% compatível');
    expect(texto).toContain('Alta');
    expect(texto).toContain('Conhece o setor PetTech');
  });

  it('classifica em Média e Baixa', () => {
    expect((montar(50).nativeElement as HTMLElement).textContent).toContain('Média');
    expect((montar(10).nativeElement as HTMLElement).textContent).toContain('Baixa');
  });

  it('não aparece quando não há cálculo (lista comum)', () => {
    expect((montar(null).nativeElement as HTMLElement).textContent!.trim()).toBe('');
  });
});
