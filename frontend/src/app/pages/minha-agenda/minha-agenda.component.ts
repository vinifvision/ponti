import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AgendaService } from '../../agenda/agenda.service';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { MentorService } from '../../mentores/mentor.service';
import { DiaSemana, FaixaDisponibilidade } from '../../models';
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

// "Minha agenda" do mentor: os horários em que ele atende na semana.
// As startups veem no calendário os blocos livres gerados a partir daqui.
@Component({
  selector: 'app-minha-agenda',
  standalone: true,
  imports: [FormsModule],
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
  faixas = signal<Record<DiaSemana, Faixa[]>>(this.vazio());
  duracao = signal('');
  carregando = signal(true);
  salvando = signal(false);
  erro = signal('');
  salvo = signal(false);

  explicacao = computed(() => {
    const blocos = this.duracao() ? `, em blocos de ${this.duracao()} (a duração da sua sessão)` : '';
    return `Diga em quais horários você atende. As startups veem os horários livres no seu calendário${blocos}.`;
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
      next: (m) => this.duracao.set(formatarDuracao(m.duracaoSessaoMinutos ?? 60)),
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
      for (const f of this.faixas()[dia]) {
        if (!f.horaInicio || !f.horaFim || f.horaFim <= f.horaInicio) {
          this.erro.set(`${nome}: o horário de término precisa ser depois do início.`);
          return;
        }
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

  private somarHoras(horario: string, horas: number): string {
    const [h, m] = horario.split(':').map(Number);
    const total = Math.min(h + horas, 23);
    return `${String(total).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  }

  private vazio(): Record<DiaSemana, Faixa[]> {
    return { MONDAY: [], TUESDAY: [], WEDNESDAY: [], THURSDAY: [], FRIDAY: [], SATURDAY: [], SUNDAY: [] };
  }
}
