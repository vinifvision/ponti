import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { Location } from '@angular/common';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { MentorService } from '../../mentores/mentor.service';
import { AvaliacaoPublica, Certificacao, Mentor } from '../../models';
import { CompatibilidadeComponent } from '../../shared/compatibilidade/compatibilidade.component';
import { lerCompatibilidadeDaNavegacao } from '../../shared/compatibilidade/compatibilidade.utils';
import { EstrelasComponent } from '../../shared/estrelas/estrelas.component';
import { TemasChipsComponent } from '../../shared/temas-chips/temas-chips.component';
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
// cabeçalho, preço e duração, Agendar, Sobre mim com vídeo, Experiência e Redes de contato.
@Component({
  selector: 'app-perfil-mentor',
  standalone: true,
  imports: [IconeComponent, RouterLink, EstrelasComponent, TemasChipsComponent, CompatibilidadeComponent],
  templateUrl: './perfil-mentor.component.html',
})
export class PerfilMentorComponent implements OnInit {
  // Vem da rota /mentores/:id/perfil (withComponentInputBinding)
  id = input.required<string>();

  private mentorService = inject(MentorService);
  private sessao = inject(SessaoService);
  private sanitizer = inject(DomSanitizer);
  private location = inject(Location);

  mentor = signal<Mentor | null>(null);
  certificacoes = signal<Certificacao[]>([]);
  avaliacoes = signal<AvaliacaoPublica[]>([]);
  carregando = signal(true);
  erro = signal('');

  readonly formatarPeriodo = formatarPeriodo;
  readonly compat = lerCompatibilidadeDaNavegacao();
  ehStartup = computed(() => this.sessao.startupId() !== null);
  // true quando o mentor logado está olhando o próprio perfil (rota /perfil): aparece "Editar perfil"
  proprio = computed(() => {
    const logado = this.sessao.mentorId();
    return logado !== null && logado === this.mentor()?.id;
  });
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

    this.mentorService.avaliacoes(mentorId).subscribe({
      next: (lista) => this.avaliacoes.set(lista),
      error: () => {},
    });

    this.mentorService.certificacoes(mentorId).subscribe({
      next: (lista) => this.certificacoes.set(lista),
      error: () => {}, // sem certificações, a seção só não aparece
    });
  }

  iniciaisDe(nome: string): string {
    return iniciais(nome);
  }

  dataAvaliacao(instante: string): string {
    return new Date(instante).toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  voltar() {
    this.location.back();
  }
}
