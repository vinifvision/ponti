import { Injectable, signal } from '@angular/core';

// Compartilha o texto digitado na busca da topbar com as páginas.
@Injectable({ providedIn: 'root' })
export class BuscaService {
  readonly termo = signal('');
}
