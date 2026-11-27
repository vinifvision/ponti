import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { AcaoAuditoria, Pagina, RegistroAuditoria } from '../admin.models';
import { AdminService } from '../admin.service';
import { dataHoraDoInstante } from '../admin.utils';

// Registro de auditoria: tudo o que os admins fizeram, do mais recente para o mais antigo.
@Component({
  selector: 'app-registro-admin',
  standalone: true,
  template: `
    <header class="surgir">
      <h1 class="text-3xl font-semibold tracking-tight text-ponti-marinho">Registro</h1>
      <p class="mt-1 text-gray-500">Quem fez o quê, com quem, quando e por quê. Nada aqui pode ser editado ou apagado.</p>
    </header>

    @if (erro()) {
      <div role="alert" class="mt-6 rounded-2xl bg-red-50 p-5 text-red-800 ring-1 ring-red-200">{{ erro() }}</div>
    }

    <section class="surgir mt-6 rounded-3xl bg-white p-6 shadow-sm ring-1 ring-gray-100 transition-opacity sm:p-8"
             [class.opacity-60]="carregando()" [attr.aria-busy]="carregando()" style="animation-delay: 80ms">
      @if (resultado(); as r) {
        <ol class="relative space-y-6 border-l-2 border-ponti-lilas/50 pl-6">
          @for (x of r.itens; track x.id; let i = $index) {
            <li class="surgir relative" [style.animation-delay.ms]="40 * i">
              <span class="absolute -left-[31px] top-1 size-3.5 rounded-full ring-4 ring-white"
                    [class]="x.acao === 'BLOQUEAR_USUARIO' ? 'bg-red-500' : 'bg-green-500'" aria-hidden="true"></span>
              <p class="text-xs font-medium uppercase tracking-wide text-gray-400">{{ quando(x.criadoEm) }}</p>
              <p class="mt-1 text-gray-900">
                <span class="mr-1 rounded-full px-2 py-0.5 text-xs font-semibold"
                      [class]="x.acao === 'BLOQUEAR_USUARIO' ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700'">
                  {{ rotulo(x.acao) }}
                </span>
                <strong class="font-semibold">{{ x.alvoDescricao ?? 'Conta excluída' }}</strong>
              </p>
              @if (x.motivo) {
                <p class="mt-1 text-sm text-gray-600">Motivo: {{ x.motivo }}</p>
              }
              <p class="mt-1 text-xs text-gray-500">Por {{ x.autorEmail }}</p>
            </li>
          } @empty {
            <li class="py-8 text-gray-500">Nenhuma ação registrada ainda. Quando você bloquear ou desbloquear uma conta, ela aparece aqui.</li>
          }
        </ol>

        @if (r.total > r.tamanho) {
          <nav aria-label="Páginas" class="mt-8 flex items-center justify-between text-sm">
            <button type="button" (click)="carregar(r.pagina - 1)" [disabled]="r.pagina === 0"
                    class="cursor-pointer rounded-lg px-3 py-1.5 font-medium text-ponti-roxo hover:bg-ponti-lilas/20 disabled:cursor-default disabled:text-gray-300">
              Mais recentes
            </button>
            <span class="text-gray-500">Página {{ r.pagina + 1 }}</span>
            <button type="button" (click)="carregar(r.pagina + 1)" [disabled]="(r.pagina + 1) * r.tamanho >= r.total"
                    class="cursor-pointer rounded-lg px-3 py-1.5 font-medium text-ponti-roxo hover:bg-ponti-lilas/20 disabled:cursor-default disabled:text-gray-300">
              Mais antigos
            </button>
          </nav>
        }
      } @else if (carregando()) {
        <div class="space-y-4" aria-hidden="true">
          @for (i of [1, 2, 3]; track i) {
            <div class="h-16 animate-pulse rounded-xl bg-gray-100"></div>
          }
        </div>
      }
    </section>
  `,
  styles: `
    @keyframes surgir {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: none; }
    }
    .surgir { animation: surgir 0.4s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
    @media (prefers-reduced-motion: reduce) { .surgir { animation: none; } }
  `,
})
export class RegistroComponent implements OnInit {
  private admin = inject(AdminService);

  protected resultado = signal<Pagina<RegistroAuditoria> | null>(null);
  protected carregando = signal(true);
  protected erro = signal('');

  ngOnInit() {
    this.carregar(0);
  }

  carregar(pagina: number) {
    this.carregando.set(true);
    this.admin.auditoria(pagina).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar o registro.'));
        this.carregando.set(false);
      },
    });
  }

  protected quando = dataHoraDoInstante;

  protected rotulo(acao: AcaoAuditoria): string {
    return { BLOQUEAR_USUARIO: 'Bloqueou', DESBLOQUEAR_USUARIO: 'Desbloqueou' }[acao];
  }
}
