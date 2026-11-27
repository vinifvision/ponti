import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { Atencao, Dashboard, TipoAtencao } from '../admin.models';
import { AdminService } from '../admin.service';
import { PERIODOS, ROTULO_DESTAQUE, ROTULO_ETAPA, dataCurta, decisoes, largura, ultimosDias, variacao } from '../admin.utils';

// Tooltip que segue o mouse nos gráficos
interface Dica {
  titulo: string;
  linhas: string[];
  x: number;
  y: number;
}

// Cores dos gráficos (validadas para daltonismo e contraste com o script do guia de visualização):
// uma série usa o roxo da Ponti; "startups x mentores" usa índigo e roxo.
export const COR_SERIE = '#a46ff5';
export const COR_STARTUPS = '#3d38c4';
export const COR_MENTORES = '#a46ff5';

// Última posição da rolagem do painel, para voltar no mesmo lugar com o "voltar" do navegador
let rolagemSalva = 0;

// Painel do admin: cada bloco responde uma pergunta e termina com uma sugestão do que fazer.
@Component({
  selector: 'app-painel',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './painel.component.html',
  styles: `
    @keyframes surgir {
      from { opacity: 0; transform: translateY(12px); }
      to { opacity: 1; transform: none; }
    }
    .surgir { animation: surgir 0.5s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
    @media (prefers-reduced-motion: reduce) {
      .surgir { animation: none; }
      .barra { transition: none !important; }
    }
  `,
})
export class PainelComponent implements OnInit, OnDestroy {
  private admin = inject(AdminService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  // Veio pelo botão "voltar" (ou "avançar") do navegador?
  private voltou = this.router.getCurrentNavigation()?.trigger === 'popstate';
  private restaurarRolagem = this.voltou;

  protected readonly periodos = PERIODOS;
  protected readonly corSerie = COR_SERIE;
  protected readonly corStartups = COR_STARTUPS;
  protected readonly corMentores = COR_MENTORES;

  protected dias = signal<number | 'personalizado'>(30);
  protected inicioPersonalizado = '';
  protected fimPersonalizado = '';
  protected hoje = ultimosDias(1).fim;

  protected dados = signal<Dashboard | null>(null);
  protected carregando = signal(true);
  protected erro = signal('');
  // As barras começam em zero e crescem até o valor (animação)
  protected animado = signal(false);
  protected dica = signal<Dica | null>(null);

  protected textos = computed(() => {
    const d = this.dados();
    return d ? decisoes(d) : null;
  });

  protected maiorOferta = computed(() => {
    const d = this.dados();
    if (!d) return 1;
    return Math.max(1, ...d.ofertaDemanda.map((t) => Math.max(t.startups, t.mentores)));
  });

  ngOnInit() {
    // O período fica no endereço (?periodo=90 ou ?inicio=...&fim=...), então "voltar" reabre o mesmo período
    const q = this.route.snapshot.queryParamMap;
    const periodo = q.get('periodo');
    if (q.get('inicio') && q.get('fim')) {
      this.inicioPersonalizado = q.get('inicio')!;
      this.fimPersonalizado = q.get('fim')!;
      this.dias.set('personalizado');
    } else if (periodo && PERIODOS.some((p) => String(p.dias) === periodo)) {
      this.dias.set(Number(periodo));
    }
    this.areaDeRolagem()?.addEventListener('scroll', this.guardarRolagem, { passive: true });
    this.carregar();
  }

  ngOnDestroy() {
    this.areaDeRolagem()?.removeEventListener('scroll', this.guardarRolagem);
  }

  // Guarda a posição a cada rolagem (ao sair da tela o conteúdo já foi trocado e a posição se perde)
  private guardarRolagem = () => {
    if (!this.restaurarRolagem) rolagemSalva = this.areaDeRolagem()?.scrollTop ?? 0;
  };

  escolherDias(dias: number) {
    this.dias.set(dias);
    this.guardarNoEndereco();
    this.carregar();
  }

  abrirPersonalizado() {
    const d = this.dados();
    this.inicioPersonalizado = d?.periodo.inicio ?? ultimosDias(30).inicio;
    this.fimPersonalizado = d?.periodo.fim ?? this.hoje;
    this.dias.set('personalizado');
  }

  aplicarPersonalizado() {
    if (!this.inicioPersonalizado || !this.fimPersonalizado) return;
    this.guardarNoEndereco();
    this.carregar();
  }

  // Troca o endereço sem criar uma entrada nova no histórico (o "voltar" continua levando para a tela anterior)
  private guardarNoEndereco() {
    const escolha = this.dias();
    const queryParams = escolha === 'personalizado'
      ? { periodo: null, inicio: this.inicioPersonalizado, fim: this.fimPersonalizado }
      : { periodo: escolha === 30 ? null : escolha, inicio: null, fim: null };
    this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge', replaceUrl: true });
  }

  // A área que rola é a do AdminShellComponent (a página em si não rola)
  private areaDeRolagem(): HTMLElement | null {
    return document.getElementById('admin-rolagem');
  }

  carregar() {
    const escolha = this.dias();
    const { inicio, fim } = escolha === 'personalizado'
      ? { inicio: this.inicioPersonalizado, fim: this.fimPersonalizado }
      : ultimosDias(escolha);
    this.carregando.set(true);
    this.erro.set('');
    this.admin.dashboard(inicio, fim).subscribe({
      next: (d) => {
        this.animado.set(false);
        this.dados.set(d);
        this.carregando.set(false);
        // Espera o navegador desenhar as barras em zero para então crescê-las
        requestAnimationFrame(() => requestAnimationFrame(() => {
          this.animado.set(true);
          if (this.restaurarRolagem) {
            this.restaurarRolagem = false;
            this.areaDeRolagem()?.scrollTo({ top: rolagemSalva });
          }
        }));
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar o painel. Tente de novo.'));
        this.carregando.set(false);
      },
    });
  }

  // ------------------------------------------------------------ apoio do template

  protected variacao = variacao;
  protected largura = largura;
  protected dataCurta = dataCurta;
  protected rotuloEtapa = ROTULO_ETAPA;
  protected rotuloDestaque = ROTULO_DESTAQUE;
  protected Math = Math;

  protected mostrarDica(evento: MouseEvent, titulo: string, ...linhas: string[]) {
    this.dica.set({ titulo, linhas, x: evento.clientX, y: evento.clientY });
  }

  protected esconderDica() {
    this.dica.set(null);
  }

  protected rotuloAtencao(tipo: TipoAtencao): string {
    return { MENTOR_MAL_AVALIADO: 'Mal avaliado', MENTOR_SEM_RESPOSTA: 'Sem resposta', STARTUP_SEM_CONEXAO: 'Sem conexão' }[tipo];
  }

  protected classeAtencao(tipo: TipoAtencao): string {
    return {
      MENTOR_MAL_AVALIADO: 'bg-red-50 text-red-700 ring-red-200',
      MENTOR_SEM_RESPOSTA: 'bg-amber-50 text-amber-800 ring-amber-200',
      STARTUP_SEM_CONEXAO: 'bg-indigo-50 text-indigo-800 ring-indigo-200',
    }[tipo];
  }

  protected quemE(a: Atencao): string {
    return a.perfil === 'MENTOR' ? 'Mentor' : 'Startup';
  }

  // Circunferência do anel de "entregas no prazo" (raio 34)
  protected readonly circunferencia = 2 * Math.PI * 34;
}
