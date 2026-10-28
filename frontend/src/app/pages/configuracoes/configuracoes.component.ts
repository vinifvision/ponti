import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { MentorService } from '../../mentores/mentor.service';
import { StartupService } from '../../startups/startup.service';

// Configurações da conta: dados de acesso, sair e excluir a conta (com confirmação digitada).
@Component({
  selector: 'app-configuracoes',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="max-w-3xl pb-12 pt-6">
      <h1 class="text-3xl font-medium sm:text-4xl">Configurações</h1>

      <div class="mt-8 rounded-xl border border-gray-200 bg-white p-6">
        <h2 class="text-lg font-semibold">Conta</h2>
        <dl class="mt-4 grid gap-4 sm:grid-cols-2">
          <div><dt class="text-sm text-gray-500">E-mail de acesso</dt><dd class="font-medium">{{ email() }}</dd></div>
          <div><dt class="text-sm text-gray-500">Tipo de conta</dt><dd class="font-medium">{{ tipo() }}</dd></div>
        </dl>
        <button type="button" (click)="sair()" class="mt-6 cursor-pointer rounded-lg border border-gray-300 px-5 py-2 font-medium text-gray-700 transition hover:bg-gray-50">
          Sair da conta
        </button>
      </div>

      <div class="mt-6 rounded-xl border border-gray-200 bg-white p-6">
        <h2 class="text-lg font-semibold">Privacidade (LGPD)</h2>
        <p class="mt-2 text-gray-600">
          Seus dados de contato não aparecem para outros usuários. As sessões acontecem pelo link do Google Meet
          enviado pelo mentor, e do cartão guardamos só a bandeira e os 4 últimos dígitos.
        </p>
      </div>

      <div class="mt-6 rounded-xl border border-red-200 bg-red-50/50 p-6">
        <h2 class="text-lg font-semibold text-red-800">Excluir conta</h2>
        <p class="mt-2 text-gray-700">
          Apaga seu perfil, seu login e todas as mentorias, avaliações e sugestões ligadas a ele. Não dá para desfazer.
        </p>
        <label class="mt-4 grid max-w-sm gap-1">
          <span class="text-sm text-gray-700">Para confirmar, digite <strong>EXCLUIR</strong></span>
          <input name="confirmacao" [(ngModel)]="confirmacao" class="campo" autocomplete="off" />
        </label>
        @if (erro()) {
          <p class="mt-3 text-red-700" role="alert">{{ erro() }}</p>
        }
        <button
          type="button"
          (click)="excluir()"
          [disabled]="confirmacao !== 'EXCLUIR' || excluindo()"
          class="mt-4 cursor-pointer rounded-lg bg-red-600 px-5 py-2 font-medium text-white transition hover:bg-red-700 disabled:cursor-default disabled:opacity-40"
        >
          {{ excluindo() ? 'Excluindo...' : 'Excluir minha conta' }}
        </button>
      </div>
    </section>
  `,
})
export class ConfiguracoesComponent {
  private sessao = inject(SessaoService);
  private mentorService = inject(MentorService);
  private startupService = inject(StartupService);
  private router = inject(Router);

  email = computed(() => this.sessao.usuario()?.email ?? '');
  tipo = computed(() => (this.sessao.tipo() === 'MENTOR' ? 'Mentor' : 'Startup'));
  confirmacao = '';
  excluindo = signal(false);
  erro = signal('');

  sair() {
    this.sessao.sair();
    this.router.navigateByUrl('/bem-vindo');
  }

  excluir() {
    const mentorId = this.sessao.mentorId();
    const startupId = this.sessao.startupId();
    const pedido: Observable<void> | null = mentorId !== null ? this.mentorService.excluir(mentorId) : startupId !== null ? this.startupService.excluir(startupId) : null;
    if (!pedido || this.confirmacao !== 'EXCLUIR') return;
    this.excluindo.set(true);
    pedido.subscribe({
      next: () => this.sair(),
      error: (e: HttpErrorResponse) => {
        this.excluindo.set(false);
        this.erro.set(mensagemDeErro(e, 'Não foi possível excluir a conta.'));
      },
    });
  }
}
