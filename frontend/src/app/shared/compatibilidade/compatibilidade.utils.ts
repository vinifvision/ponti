import { inject } from '@angular/core';
import { Router } from '@angular/router';

export interface CompatibilidadeNavegacao {
  pontos: number;
  motivos: string[];
}

// Deve ser chamada no construtor/inicializador do componente do perfil. O router só grava
// history.state depois que a navegação termina, então durante a criação do componente o valor
// vem de getCurrentNavigation(); history.state cobre o recarregar da página.
export function lerCompatibilidadeDaNavegacao(): CompatibilidadeNavegacao | null {
  const state =
    inject(Router).getCurrentNavigation()?.extras.state ?? (typeof history !== 'undefined' ? history.state : null);
  if (typeof state?.compatibilidade !== 'number') return null;
  return { pontos: state.compatibilidade, motivos: Array.isArray(state.motivos) ? state.motivos : [] };
}
