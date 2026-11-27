import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AgendaService } from '../../agenda/agenda.service';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { MentorService } from '../../mentores/mentor.service';
import { DiaSemana, ExcecaoDia, FaixaDisponibilidade } from '../../models';
import { diaSemanaDe, PASSO_MINUTOS, validarFaixas } from '../../agenda/agenda.utils';
import { dataIso, dataPorExtenso, nomeDoMes } from '../../shared/data.utils';
import { IconeComponent } from '../../shared/icone/icone.component';
import { formatarDuracao } from '../../shared/mentoria.utils';

interface Faixa {
  horaInicio: string; // "09:00"
  horaFim: string;
}

const DIAS: { dia: DiaSemana; nome: string }[] = [
  { dia: 'MONDAY', nome: 'Segunda-feira' },
  { dia: 'TUESDAY', nome: 'Terça-feira' },
  { dia: 'WEDNESDAY', nome: 'Quarta-feira' },
  { dia: 'THURSDAY', nome: 'Quinta-feira' },
  { dia: 'FRIDAY', nome: 'Sexta-feira' },
  { dia: 'SATURDAY', nome: 'Sábado' },
  { dia: 'SUNDAY', nome: 'Domingo' },
];

type ModoDia = 'padrao' | 'indisponivel' | 'personalizado';

interface DiaCalendario {
  iso: string;
  numero: number;
  doMes: boolean; // false = dia do mês anterior/seguinte que só completa a grade
  passado: boolean;
  atendePadrao: boolean; // o horário semanal tem alguma faixa nesse dia da semana
  excecao: ExcecaoDia | null;
}

// "Minha agenda" do mentor, em duas camadas (como Calendly e Cal.com):
//  1. horário semanal padrão (repete toda semana);
//  2. datas específicas: um ajuste de UM dia (folga ou horário diferente) que vale no lugar do padrão.
// As startups veem no calendário os blocos livres gerados a partir das duas.
@Component({
  selector: 'app-minha-agenda',
  standalone: true,
  imports: [FormsModule, IconeComponent],
  templateUrl: './minha-agenda.component.html',
})
export class MinhaAgendaComponent implements OnInit {
  // ?novo=1 quando o mentor acabou de se cadastrar
  novo = input<string>();

  private agendaService = inject(AgendaService);
  private mentorService = inject(MentorService);
  private sessao = inject(SessaoService);
  private router = inject(Router);

  readonly dias = DIAS;
  readonly diasDaSemana = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'];
  readonly passoSegundos = PASSO_MINUTOS * 60;
  faixas = signal<Record<DiaSemana, Faixa[]>>(this.vazio());
  duracao = signal('');
  duracaoMinutos = signal(60);
  carregando = signal(true);
  salvando = signal(false);
  erro = signal('');
  salvo = signal(false);

  explicacao = computed(() => {
    const blocos = this.duracao() ? `, em blocos de ${this.duracao()} (a duração da sua sessão)` : '';
    return `Diga em quais horários você atende. As startups veem os horários livres no seu calendário${blocos}.`;
  });

  // ---------- Datas específicas ----------
  mes = signal(this.primeiroDoMes(new Date()));
  excecoes = signal<Record<string, ExcecaoDia>>({});
  carregandoExcecoes = signal(false);
  diaEditando = signal<string | null>(null);
  modoDia = signal<ModoDia>('padrao');
  faixasDia = signal<Faixa[]>([]);
  salvandoDia = signal(false);
  erroDia = signal('');
  avisoDia = signal('');

  tituloMes = computed(() => `${nomeDoMes(this.mes().getMonth())} de ${this.mes().getFullYear()}`);
  podeVoltarMes = computed(() => this.mes() > this.primeiroDoMes(new Date()));
  tituloDia = computed(() => {
    const iso = this.diaEditando();
    return iso ? dataPorExtenso(iso) : '';
  });
  excecaoDoDia = computed(() => {
    const iso = this.diaEditando();
    return iso ? (this.excecoes()[iso] ?? null) : null;
  });

  // Grade de 6 semanas começando no domingo
  grade = computed<DiaCalendario[]>(() => {
    const primeiro = this.mes();
    const inicio = new Date(primeiro);
    inicio.setDate(1 - primeiro.getDay());
    const hoje = dataIso(new Date());
    const porDia = this.faixas();
    const ajustes = this.excecoes();
    return Array.from({ length: 42 }, (_, i) => {
      const data = new Date(inicio);
      data.setDate(inicio.getDate() + i);
      const iso = dataIso(data);
      return {
        iso,
        numero: data.getDate(),
        doMes: data.getMonth() === primeiro.getMonth(),
        passado: iso < hoje,
        atendePadrao: porDia[diaSemanaDe(iso)].length > 0,
        excecao: ajustes[iso] ?? null,
      };
    });
  });

  totalFaixas = computed(() => Object.values(this.faixas()).reduce((total, lista) => total + lista.length, 0));

  ngOnInit() {
    const mentorId = this.sessao.mentorId();
    if (mentorId === null) {
      // Só mentores têm agenda; a startup acompanha as sessões em Mentorias
      this.router.navigateByUrl('/mentorias');
      return;
    }

    this.mentorService.buscar(mentorId).subscribe({
      next: (m) => {
        this.duracao.set(formatarDuracao(m.duracaoSessaoMinutos ?? 60));
        this.duracaoMinutos.set(m.duracaoSessaoMinutos ?? 60);
      },
      error: () => {},
    });

    this.agendaService.disponibilidade(mentorId).subscribe({
      next: (lista) => {
        const porDia = this.vazio();
        for (const f of lista) {
          porDia[f.diaSemana].push({ horaInicio: f.horaInicio.slice(0, 5), horaFim: f.horaFim.slice(0, 5) });
        }
        this.faixas.set(porDia);
        this.carregando.set(false);
        this.carregarExcecoes();
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar a sua agenda.'));
        this.carregando.set(false);
      },
    });
  }

  adicionar(dia: DiaSemana) {
    this.salvo.set(false);
    this.faixas.update((atual) => {
      const lista = atual[dia];
      // Sugere a faixa seguinte à última do dia (ou 09:00 às 12:00)
      const ultima = lista[lista.length - 1];
      const nova = ultima ? { horaInicio: ultima.horaFim, horaFim: this.somarHoras(ultima.horaFim, 2) } : { horaInicio: '09:00', horaFim: '12:00' };
      return { ...atual, [dia]: [...lista, nova] };
    });
  }

  remover(dia: DiaSemana, indice: number) {
    this.salvo.set(false);
    this.faixas.update((atual) => ({ ...atual, [dia]: atual[dia].filter((_, i) => i !== indice) }));
  }

  alterou() {
    this.salvo.set(false);
  }

  salvar() {
    const mentorId = this.sessao.mentorId();
    if (mentorId === null || this.salvando()) return;

    const lista: FaixaDisponibilidade[] = [];
    for (const { dia, nome } of DIAS) {
      const problema = validarFaixas(this.faixas()[dia], this.duracaoMinutos());
      if (problema) {
        this.erro.set(`${nome}: ${problema}`);
        return;
      }
      for (const f of this.faixas()[dia]) {
        lista.push({ diaSemana: dia, horaInicio: f.horaInicio, horaFim: f.horaFim });
      }
    }

    this.salvando.set(true);
    this.erro.set('');
    this.agendaService.salvarDisponibilidade(mentorId, lista).subscribe({
      next: () => {
        this.salvando.set(false);
        this.salvo.set(true);
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.erro.set(mensagemDeErro(e, 'Não foi possível salvar. Confira se há horários sobrepostos no mesmo dia.'));
      },
    });
  }

  // ---------- Datas específicas ----------

  mudarMes(delta: number) {
    const novo = new Date(this.mes());
    novo.setMonth(novo.getMonth() + delta);
    this.mes.set(novo);
    this.fecharDia();
    this.carregarExcecoes();
  }

  escolherDia(dia: DiaCalendario) {
    if (!dia.doMes || dia.passado) return;
    const ex = dia.excecao;
    this.diaEditando.set(dia.iso);
    this.erroDia.set('');
    this.avisoDia.set('');
    if (ex) {
      this.modoDia.set(ex.indisponivel ? 'indisponivel' : 'personalizado');
      this.faixasDia.set(ex.faixas.map((f) => this.cortar(f)));
    } else {
      this.modoDia.set('padrao');
      this.faixasDia.set(this.faixasDoPadrao(dia.iso));
    }
  }

  fecharDia() {
    this.diaEditando.set(null);
    this.erroDia.set('');
    this.avisoDia.set('');
  }

  escolherModo(modo: ModoDia) {
    this.modoDia.set(modo);
    this.erroDia.set('');
    this.avisoDia.set('');
    // Ao personalizar, parte do horário padrão do dia para o mentor só ajustar
    if (modo === 'personalizado' && this.faixasDia().length === 0) {
      const iso = this.diaEditando();
      const base = iso ? this.faixasDoPadrao(iso) : [];
      this.faixasDia.set(base.length ? base : [{ horaInicio: '09:00', horaFim: '12:00' }]);
    }
  }

  adicionarFaixaDia() {
    this.avisoDia.set('');
    this.faixasDia.update((lista) => {
      const ultima = lista[lista.length - 1];
      const nova = ultima ? { horaInicio: ultima.horaFim, horaFim: this.somarHoras(ultima.horaFim, 2) } : { horaInicio: '09:00', horaFim: '12:00' };
      return [...lista, nova];
    });
  }

  removerFaixaDia(indice: number) {
    this.avisoDia.set('');
    this.faixasDia.update((lista) => lista.filter((_, i) => i !== indice));
  }

  alterouDia() {
    this.avisoDia.set('');
  }

  salvarDia() {
    const mentorId = this.sessao.mentorId();
    const iso = this.diaEditando();
    if (mentorId === null || iso === null || this.salvandoDia()) return;

    const modo = this.modoDia();
    this.erroDia.set('');
    this.avisoDia.set('');

    if (modo === 'padrao') {
      // Sem ajuste guardado não há o que fazer; com ajuste, apaga e a data volta ao horário semanal
      if (!this.excecaoDoDia()) {
        this.fecharDia();
        return;
      }
      this.salvandoDia.set(true);
      this.agendaService.removerExcecao(mentorId, iso).subscribe({
        next: () => {
          this.salvandoDia.set(false);
          this.excecoes.update((atual) => {
            const { [iso]: _removida, ...resto } = atual;
            return resto;
          });
          this.avisoDia.set('Pronto! Esse dia voltou a seguir o horário semanal.');
        },
        error: (e: HttpErrorResponse) => this.falhaDia(e),
      });
      return;
    }

    let faixas: Faixa[] = [];
    if (modo === 'personalizado') {
      if (this.faixasDia().length === 0) {
        this.erroDia.set('Adicione pelo menos um horário, ou escolha "Não atendo neste dia".');
        return;
      }
      const problema = validarFaixas(this.faixasDia(), this.duracaoMinutos());
      if (problema) {
        this.erroDia.set(problema);
        return;
      }
      faixas = this.faixasDia().map((f) => this.cortar(f));
    }

    this.salvandoDia.set(true);
    this.agendaService.salvarExcecao(mentorId, iso, { indisponivel: modo === 'indisponivel', faixas }).subscribe({
      next: (salva) => {
        this.salvandoDia.set(false);
        this.excecoes.update((atual) => ({ ...atual, [iso]: salva }));
        this.avisoDia.set(modo === 'indisponivel' ? 'Pronto! Você não atende nesse dia.' : 'Pronto! Horários desse dia atualizados.');
      },
      error: (e: HttpErrorResponse) => this.falhaDia(e),
    });
  }

  private falhaDia(e: HttpErrorResponse) {
    this.salvandoDia.set(false);
    this.erroDia.set(mensagemDeErro(e, 'Não foi possível salvar esse dia. Tente de novo.'));
  }

  // Busca os ajustes da grade visível (inclui os dias do mês vizinho que completam as semanas)
  private carregarExcecoes() {
    const mentorId = this.sessao.mentorId();
    if (mentorId === null) return;
    const g = this.grade();
    this.carregandoExcecoes.set(true);
    this.agendaService.excecoes(mentorId, g[0].iso, g[g.length - 1].iso).subscribe({
      next: (lista) => {
        // Mantém os ajustes de outros meses já carregados e troca os da janela atual
        this.excecoes.update((atual) => {
          const resto = Object.fromEntries(Object.entries(atual).filter(([iso]) => iso < g[0].iso || iso > g[g.length - 1].iso));
          for (const ex of lista) resto[ex.data] = { ...ex, faixas: ex.faixas.map((f) => this.cortar(f)) };
          return resto;
        });
        this.carregandoExcecoes.set(false);
      },
      error: () => this.carregandoExcecoes.set(false),
    });
  }

  private faixasDoPadrao(iso: string): Faixa[] {
    return this.faixas()[diaSemanaDe(iso)].map((f) => ({ ...f }));
  }

  // "09:00:00" -> "09:00"
  private cortar(f: Faixa): Faixa {
    return { horaInicio: f.horaInicio.slice(0, 5), horaFim: f.horaFim.slice(0, 5) };
  }

  private primeiroDoMes(d: Date): Date {
    return new Date(d.getFullYear(), d.getMonth(), 1);
  }

  private somarHoras(horario: string, horas: number): string {
    const [h, m] = horario.split(':').map(Number);
    const total = Math.min(h + horas, 23);
    return `${String(total).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  }

  private vazio(): Record<DiaSemana, Faixa[]> {
    return { MONDAY: [], TUESDAY: [], WEDNESDAY: [], THURSDAY: [], FRIDAY: [], SATURDAY: [], SUNDAY: [] };
  }
}
