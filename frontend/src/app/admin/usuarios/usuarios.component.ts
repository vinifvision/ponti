import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import type { TipoUsuario } from '../../auth/sessao.service';
import { IconeComponent } from '../../shared/icone/icone.component';
import { Pagina, UsuarioAdmin } from '../admin.models';
import { AdminService } from '../admin.service';
import { dataDoInstante, iniciais } from '../admin.utils';

type Situacao = 'ATIVO' | 'BLOQUEADO' | null;

// Lista de usuários do admin: busca, filtros e bloquear/desbloquear (com motivo, que vai para o registro).
@Component({
  selector: 'app-usuarios-admin',
  standalone: true,
  imports: [FormsModule, IconeComponent],
  templateUrl: './usuarios.component.html',
  styles: `
    @keyframes surgir {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: none; }
    }
    .surgir { animation: surgir 0.35s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
    @media (prefers-reduced-motion: reduce) { .surgir { animation: none; } }
  `,
})
export class UsuariosComponent implements OnInit {
  private admin = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  protected readonly tipos: { valor: TipoUsuario | null; rotulo: string }[] = [
    { valor: null, rotulo: 'Todos' },
    { valor: 'STARTUP', rotulo: 'Startups' },
    { valor: 'MENTOR', rotulo: 'Mentores' },
    { valor: 'ADMIN', rotulo: 'Admins' },
  ];
  protected readonly situacoes: { valor: Situacao; rotulo: string }[] = [
    { valor: null, rotulo: 'Todas' },
    { valor: 'ATIVO', rotulo: 'Ativas' },
    { valor: 'BLOQUEADO', rotulo: 'Bloqueadas' },
  ];

  protected busca = '';
  protected tipo = signal<TipoUsuario | null>(null);
  protected situacao = signal<Situacao>(null);
  protected pagina = signal(0);
  protected resultado = signal<Pagina<UsuarioAdmin> | null>(null);
  protected carregando = signal(true);
  protected erro = signal('');
  protected aviso = signal('');

  // Janela de bloquear/desbloquear
  protected alvo = signal<UsuarioAdmin | null>(null);
  protected motivo = '';
  protected salvando = signal(false);
  protected erroJanela = signal('');
  private janela = viewChild.required<ElementRef<HTMLDialogElement>>('janela');

  private digitacao = new Subject<void>();

  constructor() {
    // Espera a pessoa parar de digitar antes de buscar
    this.digitacao.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.buscar());
  }

  ngOnInit() {
    // O painel manda ?busca=Nome no link "Ver conta"
    this.busca = this.route.snapshot.queryParamMap.get('busca') ?? '';
    this.carregar();
  }

  protected digitou() {
    this.digitacao.next();
  }

  protected buscar() {
    this.pagina.set(0);
    this.router.navigate([], { queryParams: { busca: this.busca.trim() || null }, replaceUrl: true });
    this.carregar();
  }

  protected escolherTipo(valor: TipoUsuario | null) {
    this.tipo.set(valor);
    this.buscar();
  }

  protected escolherSituacao(valor: Situacao) {
    this.situacao.set(valor);
    this.buscar();
  }

  protected irPara(pagina: number) {
    this.pagina.set(pagina);
    this.carregar();
  }

  carregar() {
    this.carregando.set(true);
    this.erro.set('');
    this.admin
      .usuarios({ busca: this.busca, tipo: this.tipo(), situacao: this.situacao(), pagina: this.pagina(), tamanho: 15 })
      .subscribe({
        next: (r) => {
          this.resultado.set(r);
          this.carregando.set(false);
        },
        error: (e: HttpErrorResponse) => {
          this.erro.set(mensagemDeErro(e, 'Não foi possível carregar os usuários.'));
          this.carregando.set(false);
        },
      });
  }

  // ------------------------------------------------------------ bloquear / desbloquear

  protected abrir(u: UsuarioAdmin) {
    this.alvo.set(u);
    this.motivo = '';
    this.erroJanela.set('');
    this.janela().nativeElement.showModal();
  }

  protected fechar() {
    this.janela().nativeElement.close();
    this.alvo.set(null);
  }

  protected confirmar() {
    const u = this.alvo();
    if (!u) return;
    if (!u.bloqueado && !this.motivo.trim()) {
      this.erroJanela.set('Escreva o motivo do bloqueio. Ele fica guardado no registro.');
      return;
    }
    this.salvando.set(true);
    const pedido = u.bloqueado ? this.admin.desbloquear(u.id, this.motivo) : this.admin.bloquear(u.id, this.motivo.trim());
    pedido.subscribe({
      next: (atualizado) => {
        this.salvando.set(false);
        this.fechar();
        this.resultado.update((r) => r && { ...r, itens: r.itens.map((x) => (x.id === atualizado.id ? atualizado : x)) });
        this.mostrarAviso(`${atualizado.nome ?? atualizado.email} foi ${atualizado.bloqueado ? 'bloqueado(a)' : 'desbloqueado(a)'}.`);
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.erroJanela.set(mensagemDeErro(e, 'Não foi possível salvar. Tente de novo.'));
      },
    });
  }

  private mostrarAviso(texto: string) {
    this.aviso.set(texto);
    setTimeout(() => this.aviso.set(''), 3500);
  }

  // ------------------------------------------------------------ apoio do template

  protected iniciais = iniciais;
  protected data = dataDoInstante;

  protected rotuloTipo(tipo: TipoUsuario): string {
    return { STARTUP: 'Startup', MENTOR: 'Mentor', ADMIN: 'Admin' }[tipo];
  }

  protected classeTipo(tipo: TipoUsuario): string {
    return {
      STARTUP: 'bg-indigo-50 text-indigo-800',
      MENTOR: 'bg-[#f3ebff] text-ponti-marinho',
      ADMIN: 'bg-gray-100 text-gray-700',
    }[tipo];
  }

  protected totalDePaginas(r: Pagina<UsuarioAdmin>): number {
    return Math.max(1, Math.ceil(r.total / r.tamanho));
  }
}
