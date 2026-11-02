import { CanDeactivateFn } from '@angular/router';

// Tela que quer confirmar antes de ser fechada (ex.: formulário com alterações não salvas).
export interface ProtegeSaida {
  podeSair(): boolean | Promise<boolean>;
}

// Uso: { path: '...', component: Tela, canDeactivate: [sairSemSalvarGuard] }
export const sairSemSalvarGuard: CanDeactivateFn<ProtegeSaida> = (tela) => tela.podeSair();
