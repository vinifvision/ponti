import { HttpErrorResponse } from '@angular/common/http';
import { Location } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AgendaService } from '../../agenda/agenda.service';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { MentorService } from '../../mentores/mentor.service';
import { Agendamento, MetodoPagamento, Mentor } from '../../models';
import { dataHoraCompleta, dataIso, dataPorExtenso, hora, nomeDoMes } from '../../shared/data.utils';
import { IconeComponent } from '../../shared/icone/icone.component';
import { formatarDuracao, formatarValorSessao } from '../../shared/mentoria.utils';
import { iniciais } from '../../shared/texto.utils';

interface DiaCalendario {
  data: Date;
  iso: string;
  doMes: boolean; // false = dia do mês anterior/seguinte que só completa a grade
  temHorario: boolean;
}

// Agendar uma mentoria (design "Agendar"): calendário do mês com os dias que têm horário livre,
// lista de horários do dia, depois resumo com pagamento SIMULADO e a confirmação do pedido.
@Component({
  selector: 'app-agendar-mentoria',
  standalone: true,
  imports: [FormsModule, RouterLink, IconeComponent],
  templateUrl: './agendar-mentoria.component.html',
})
export class AgendarMentoriaComponent implements OnInit {
  // Vem da rota /mentores/:id/agendar
  id = input.required<string>();

  private mentorService = inject(MentorService);
  private agendaService = inject(AgendaService);
  private sessao = inject(SessaoService);
  private location = inject(Location);

  readonly diasDaSemana = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'];

  mentor = signal<Mentor | null>(null);
  erro = signal('');
  carregandoHorarios = signal(false);

  // Mês mostrado no calendário (dia 1)
  mes = signal(this.primeiroDoMes(new Date()));
  horarios = signal<string[]>([]);
  diaSelecionado = signal<string | null>(null); // "2026-11-14"
  horarioSelecionado = signal<string | null>(null); // "2026-11-14T10:00:00"

  // Etapas: 1 = escolher horário, 2 = pagamento, 3 = pedido enviado
  etapa = signal<1 | 2 | 3>(1);
  metodoPagamento: MetodoPagamento = 'PIX';
  mensagem = '';
  enviando = signal(false);
  agendamento = signal<Agendamento | null>(null);

  ehStartup = computed(() => this.sessao.startupId() !== null);
  iniciais = computed(() => iniciais(this.mentor()?.nome ?? ''));
  valor = computed(() => formatarValorSessao(this.mentor()?.valorSessao ?? 0));
  gratuita = computed(() => !this.mentor()?.valorSessao);
  duracao = computed(() => formatarDuracao(this.mentor()?.duracaoSessaoMinutos ?? 60));
  tituloMes = computed(() => `${nomeDoMes(this.mes().getMonth())} de ${this.mes().getFullYear()}`);
  podeVoltarMes = computed(() => this.mes() > this.primeiroDoMes(new Date()));

  diasComHorario = computed(() => new Set(this.horarios().map((h) => h.slice(0, 10))));
  horariosDoDia = computed(() => this.horarios().filter((h) => h.startsWith(this.diaSelecionado() ?? '-')));
  textoSelecionado = computed(() => {
    const h = this.horarioSelecionado();
    return h ? `${dataPorExtenso(h)}, às ${hora(h)}` : '';
  });
  textoConfirmado = computed(() => {
    const a = this.agendamento();
    return a ? dataHoraCompleta(a.inicio) : '';
  });

  // Grade de 6 semanas começando no domingo
  grade = computed<DiaCalendario[]>(() => {
    const primeiro = this.mes();
    const inicio = new Date(primeiro);
    inicio.setDate(1 - primeiro.getDay());
    const comHorario = this.diasComHorario();
    return Array.from({ length: 42 }, (_, i) => {
      const data = new Date(inicio);
      data.setDate(inicio.getDate() + i);
      const iso = dataIso(data);
      return { data, iso, doMes: data.getMonth() === primeiro.getMonth(), temHorario: comHorario.has(iso) };
    });
  });

  ngOnInit() {
    const mentorId = Number(this.id());
    this.mentorService.buscar(mentorId).subscribe({
      next: (mentor) => {
        this.mentor.set(mentor);
        this.carregarHorarios();
      },
      error: (e: HttpErrorResponse) =>
        this.erro.set(e.status === 404 ? 'Mentor não encontrado.' : mensagemDeErro(e, 'Não foi possível carregar o mentor.')),
    });
  }

  mudarMes(delta: number) {
    const novo = new Date(this.mes());
    novo.setMonth(novo.getMonth() + delta);
    this.mes.set(novo);
    this.diaSelecionado.set(null);
    this.horarioSelecionado.set(null);
    this.carregarHorarios();
  }

  escolherDia(dia: DiaCalendario) {
    if (!dia.temHorario) return;
    this.diaSelecionado.set(dia.iso);
    this.horarioSelecionado.set(null);
  }

  escolherHorario(horario: string) {
    this.horarioSelecionado.set(horario);
  }

  irParaPagamento() {
    if (!this.horarioSelecionado()) return;
    this.erro.set('');
    this.etapa.set(2);
  }

  voltarParaCalendario() {
    this.erro.set('');
    this.etapa.set(1);
  }

  voltar() {
    this.location.back();
  }

  agendar() {
    const startupId = this.sessao.startupId();
    const mentorId = this.mentor()?.id;
    const inicio = this.horarioSelecionado();
    if (startupId === null || mentorId == null || !inicio || this.enviando()) return;

    this.enviando.set(true);
    this.erro.set('');
    this.agendaService
      .agendar({
        startupId,
        mentorId,
        inicio,
        metodoPagamento: this.gratuita() ? undefined : this.metodoPagamento,
        mensagem: this.mensagem.trim() || undefined,
      })
      .subscribe({
        next: (agendamento) => {
          this.agendamento.set(agendamento);
          this.enviando.set(false);
          this.etapa.set(3);
        },
        error: (e: HttpErrorResponse) => {
          this.enviando.set(false);
          if (e.status === 409) {
            // Alguém pegou o horário antes: volta para o calendário com a lista atualizada
            this.erro.set('Esse horário acabou de ser ocupado. Escolha outro.');
            this.horarioSelecionado.set(null);
            this.etapa.set(1);
            this.carregarHorarios();
            return;
          }
          this.erro.set(mensagemDeErro(e, 'Não foi possível agendar. Tente novamente.'));
        },
      });
  }

  private carregarHorarios() {
    const mentorId = this.mentor()?.id;
    if (mentorId == null) return;
    const primeiro = this.mes();
    const hoje = new Date();
    const de = primeiro < hoje ? hoje : primeiro;
    const ultimo = new Date(primeiro.getFullYear(), primeiro.getMonth() + 1, 0);

    this.carregandoHorarios.set(true);
    this.agendaService.horariosLivres(mentorId, dataIso(de), dataIso(ultimo)).subscribe({
      next: (lista) => {
        this.horarios.set(lista);
        this.carregandoHorarios.set(false);
        // Abre no primeiro dia com horário, para a pessoa já ver opções
        if (!this.diaSelecionado() && lista.length) {
          this.diaSelecionado.set(lista[0].slice(0, 10));
        }
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível carregar os horários.'));
        this.carregandoHorarios.set(false);
      },
    });
  }

  private primeiroDoMes(d: Date): Date {
    return new Date(d.getFullYear(), d.getMonth(), 1);
  }

  diaDoMes(dia: DiaCalendario): number {
    return dia.data.getDate();
  }

  horarioCurto(h: string): string {
    return hora(h);
  }
}
