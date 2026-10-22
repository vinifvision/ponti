import { Injectable, signal } from '@angular/core';

// Guarda quem está usando o sistema. Hoje o backend não tem login, então o id da
// startup é fixo. Quando o login existir, preencher isso a partir da resposta dele.
@Injectable({ providedIn: 'root' })
export class SessaoService {
  // TEMPORÁRIO!!: id 1 = "PetCare Digital", criada em DadosIniciais.java.
  readonly startupId = signal(1);
}
