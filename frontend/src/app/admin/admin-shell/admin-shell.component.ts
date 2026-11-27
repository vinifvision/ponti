import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SessaoService } from '../../auth/sessao.service';
import { IconeComponent, NomeIcone } from '../../shared/icone/icone.component';

interface ItemMenu {
  rotulo: string;
  rota: string;
  icone: NomeIcone;
}

// Moldura da área do admin: barra lateral lilás (no celular vira uma barra no topo) e o conteúdo ao lado.
// É separada do ShellComponent de startups e mentores: o admin tem outro menu e não usa a busca.
@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconeComponent],
  template: `
    <div class="flex h-dvh flex-col bg-gray-50 font-sans text-gray-900 md:flex-row">
      <aside
        class="flex shrink-0 items-center gap-2 border-b border-ponti-lilas/40 bg-[#f3ebff] px-4 py-3
               md:w-64 md:flex-col md:items-stretch md:gap-1 md:border-b-0 md:border-r md:px-4 md:py-6"
      >
        <a routerLink="/admin/painel" class="mr-2 shrink-0 md:mb-8 md:mr-0 md:px-3" aria-label="Ponti - painel do admin">
          <img src="logo.svg" alt="Ponti" class="hidden h-10 w-auto md:block" />
          <img src="logo-mini.svg" alt="Ponti" class="h-9 w-auto md:hidden" />
        </a>

        <p class="hidden px-3 pb-2 text-xs font-semibold uppercase tracking-widest text-ponti-marinho/60 md:block">
          Administração
        </p>
        <nav aria-label="Menu do admin" class="flex flex-1 items-center gap-1 md:flex-none md:flex-col md:items-stretch">
          @for (item of itens; track item.rota) {
            <a
              [routerLink]="item.rota"
              routerLinkActive="bg-white text-ponti-roxo shadow-sm"
              [routerLinkActiveOptions]="{ exact: false }"
              #rla="routerLinkActive"
              [attr.aria-current]="rla.isActive ? 'page' : null"
              class="flex items-center gap-3 rounded-xl px-3 py-2.5 text-base font-medium text-ponti-marinho transition-all duration-200
                     hover:bg-white/70 hover:text-ponti-roxo focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-roxo"
              [attr.title]="item.rotulo"
            >
              <app-icone [nome]="item.icone" [tamanho]="22" />
              <span class="hidden sm:inline">{{ item.rotulo }}</span>
            </a>
          }
        </nav>

        <div class="ml-auto flex items-center gap-2 md:mt-auto md:ml-0 md:flex-col md:items-stretch md:gap-3">
          <div class="hidden items-center gap-3 rounded-xl px-3 py-2 md:flex">
            <span class="grid size-9 place-items-center rounded-full bg-ponti-roxo text-sm font-semibold text-white">A</span>
            <div class="min-w-0">
              <p class="text-sm font-semibold text-ponti-marinho">Administrador</p>
              <p class="truncate text-xs text-gray-500">{{ sessao.usuario()?.email }}</p>
            </div>
          </div>
          <button
            type="button"
            (click)="sair()"
            class="flex cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 text-base font-medium text-ponti-marinho transition-colors
                   hover:bg-white/70 hover:text-ponti-roxo focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ponti-roxo"
          >
            <app-icone nome="sair" [tamanho]="22" />
            <span class="hidden sm:inline">Sair</span>
          </button>
        </div>
      </aside>

      <div id="admin-rolagem" class="relative min-h-0 flex-1 overflow-y-auto">
        <main class="mx-auto w-full max-w-7xl px-4 py-6 sm:px-8 sm:py-10">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class AdminShellComponent {
  protected sessao = inject(SessaoService);
  private router = inject(Router);

  protected itens: ItemMenu[] = [
    { rotulo: 'Painel', rota: '/admin/painel', icone: 'grafico' },
    { rotulo: 'Usuários', rota: '/admin/usuarios', icone: 'usuario' },
    { rotulo: 'Registro', rota: '/admin/registro', icone: 'agenda-relogio' },
  ];

  sair() {
    this.sessao.sair();
    this.router.navigateByUrl('/login');
  }
}
