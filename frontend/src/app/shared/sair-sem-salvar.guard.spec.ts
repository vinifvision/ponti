import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { ProtegeSaida, sairSemSalvarGuard } from './sair-sem-salvar.guard';

function perguntar(tela: ProtegeSaida) {
  return sairSemSalvarGuard(
    tela,
    {} as ActivatedRouteSnapshot,
    {} as RouterStateSnapshot,
    {} as RouterStateSnapshot,
  );
}

describe('sairSemSalvarGuard', () => {
  it('deixa sair quando a tela diz que pode', () => {
    expect(perguntar({ podeSair: () => true })).toBe(true);
  });

  it('segura a saída quando a tela diz que não pode', () => {
    expect(perguntar({ podeSair: () => false })).toBe(false);
  });

  it('espera a resposta da pessoa quando a tela pergunta de forma assíncrona', async () => {
    expect(await perguntar({ podeSair: () => Promise.resolve(false) })).toBe(false);
    expect(await perguntar({ podeSair: () => Promise.resolve(true) })).toBe(true);
  });
});
