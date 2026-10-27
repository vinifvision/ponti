import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Location } from '@angular/common';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { ConviteService } from '../../convites/convite.service';
import { MentorService } from '../../mentores/mentor.service';
import { Certificacao, Mentor } from '../../models';
import { IconeComponent } from '../../shared/icone/icone.component';
import {
  formatarDuracao,
  formatarPeriodo,
  formatarValorSessao,
  idVideoYoutube,
  linkSeguro,
} from '../../shared/mentoria.utils';
import { iniciais } from '../../shared/texto.utils';

// Página pública do mentor (design "Mentores recomendados > perfil"):
// cabeçalho, preço e duração, Conectar, Sobre mim com vídeo, Experiência e Redes de contato.
@Component({
  selector: 'app-perfil-mentor',
  standalone: true,
  imports: [IconeComponent],
  templateUrl: './perfil-mentor.component.html',
})
export class PerfilMentorComponent implements OnInit {
  // Vem da rota /mentores/:id/perfil (withComponentInputBinding)
  id = input.required<string>();

  private mentorService = inject(MentorService);
  private conviteService = inject(ConviteService);
  private sessao = inject(SessaoService);
  private sanitizer = inject(DomSanitizer);
  private location = inject(Location);

  mentor = signal<Mentor | null>(null);
  certificacoes = signal<Certificacao[]>([]);
  carregando = signal(true);
  erro = signal('');
  erroConexao = signal('');
  conectado = signal(false);
  enviando = signal(false);

  readonly formatarPeriodo = formatarPeriodo;
  ehStartup = computed(() => this.sessao.startupId() !== null);
  iniciais = computed(() => iniciais(this.mentor()?.nome ?? ''));
  valor = computed(() => formatarValorSessao(this.mentor()?.valorSessao));
  duracao = computed(() => formatarDuracao(this.mentor()?.duracaoSessaoMinutos));
  linkedin = computed(() => linkSeguro(this.mentor()?.linkedinUrl));

  // Só monta o player com o id do vídeo do YouTube; qualquer outro link é ignorado.
  video = computed<SafeResourceUrl | null>(() => {
    const id = idVideoYoutube(this.mentor()?.videoUrl);
    return id ? this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube-nocookie.com/embed/${id}`) : null;
  });

  ngOnInit() {
    const mentorId = Number(this.id());
    if (!Number.isInteger(mentorId)) {
      this.erro.set('Mentor não encontrado.');
      this.carregando.set(false);
      return;
    }

    this.mentorService.buscar(mentorId).subscribe({
      next: (mentor) => {
        this.mentor.set(mentor);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(e.status === 404 ? 'Mentor não encontrado.' : mensagemDeErro(e, 'Não foi possível carregar o perfil.'));
        this.carregando.set(false);
      },
    });

    this.mentorService.certificacoes(mentorId).subscribe({
      next: (lista) => this.certificacoes.set(lista),
      error: () => {}, // sem certificações, a seção só não aparece
    });

    const startupId = this.sessao.startupId();
    if (startupId !== null) {
      this.conviteService.listarEnviados(startupId).subscribe({
        next: (convites) => this.conectado.set(convites.some((c) => c.mentorId === mentorId)),
        error: () => {},
      });
    }
  }

  voltar() {
    this.location.back();
  }

  conectar() {
    const startupId = this.sessao.startupId();
    const mentorId = this.mentor()?.id;
    if (startupId === null || mentorId == null || this.conectado() || this.enviando()) return;

    this.enviando.set(true);
    this.erroConexao.set('');
    this.conviteService.criar({ startupId, mentorId }).subscribe({
      next: () => {
        this.conectado.set(true);
        this.enviando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.enviando.set(false);
        if (e.status === 409) {
          this.conectado.set(true); // o convite já existia
          return;
        }
        this.erroConexao.set(mensagemDeErro(e, 'Não foi possível enviar a solicitação. Tente novamente.'));
      },
    });
  }
}
