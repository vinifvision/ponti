import { HttpErrorResponse } from '@angular/common/http';
import { Location } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { ConviteService } from '../../convites/convite.service';
import { Certificacao, Convite, Startup } from '../../models';
import { IconeComponent } from '../../shared/icone/icone.component';
import { NIVEL_MAXIMO, nomeDoNivel } from '../../shared/maturidade.utils';
import { formatarPeriodo, idVideoYoutube, linkSeguro } from '../../shared/mentoria.utils';
import { iniciais } from '../../shared/texto.utils';
import { StartupService } from '../../startups/startup.service';

// Página pública da startup (design "Perfil - Startup"), vista pelo mentor: cabeçalho com segmento e
// maturidade, pitch em vídeo, texto sobre a startup, premiações/certificações e redes de contato.
// O site fica de fora do perfil público.
// Ainda sem dado no backend, então ficaram de fora: o selo "Startup Verificada" e o segundo botão do Figma.
@Component({
  selector: 'app-perfil-startup',
  standalone: true,
  imports: [IconeComponent, RouterLink],
  templateUrl: './perfil-startup.component.html',
})
export class PerfilStartupComponent implements OnInit {
  // Vem da rota /startups/:id/perfil (withComponentInputBinding)
  id = input.required<string>();

  private startupService = inject(StartupService);
  private conviteService = inject(ConviteService);
  private sessao = inject(SessaoService);
  private sanitizer = inject(DomSanitizer);
  private location = inject(Location);

  startup = signal<Startup | null>(null);
  certificacoes = signal<Certificacao[]>([]);
  convite = signal<Convite | null>(null);
  carregando = signal(true);
  erro = signal('');
  erroConvite = signal('');
  enviando = signal(false);

  readonly nivelMaximo = NIVEL_MAXIMO;
  readonly formatarPeriodo = formatarPeriodo;
  ehMentor = computed(() => this.sessao.mentorId() !== null);
  // true quando a startup logada está olhando o próprio perfil (rota /perfil): aparece "Editar perfil"
  proprio = computed(() => {
    const logada = this.sessao.startupId();
    return logada !== null && logada === this.startup()?.id;
  });
  iniciais = computed(() => iniciais(this.startup()?.nome ?? ''));
  nivel = computed(() => this.startup()?.estagioMaturidade ?? null);
  nomeNivel = computed(() => nomeDoNivel(this.nivel()));
  linkedin = computed(() => linkSeguro(this.startup()?.linkedinUrl));

  // O cabeçalho mostra só o começo da descrição (3 linhas). O texto completo vem depois do pitch,
  // mas só quando tem mais conteúdo do que cabe no resumo, para não repetir uma descrição curta.
  textoCompleto = computed(() => {
    const d = this.startup()?.descricao?.trim() ?? '';
    return d.length > 180 || d.includes('\n') ? d : '';
  });
  local = computed(() => [this.startup()?.cidade, this.startup()?.estado].filter(Boolean).join(' - '));

  // Só monta o player com o id do vídeo do YouTube; qualquer outro link é ignorado.
  video = computed<SafeResourceUrl | null>(() => {
    const id = idVideoYoutube(this.startup()?.videoPitchUrl);
    return id ? this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube-nocookie.com/embed/${id}`) : null;
  });

  textoBotao = computed(() => {
    const c = this.convite();
    if (this.enviando()) return 'Enviando...';
    if (!c) return 'Sugerir mentoria';
    if (c.status === 'ACEITO') return 'Ela agendou com você';
    if (c.status === 'RECUSADO') return 'Sugestão dispensada';
    return 'Sugestão enviada';
  });

  ngOnInit() {
    const startupId = Number(this.id());
    if (!Number.isInteger(startupId)) {
      this.erro.set('Startup não encontrada.');
      this.carregando.set(false);
      return;
    }

    this.startupService.buscar(startupId).subscribe({
      next: (startup) => {
        this.startup.set(startup);
        this.carregando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(e.status === 404 ? 'Startup não encontrada.' : mensagemDeErro(e, 'Não foi possível carregar o perfil.'));
        this.carregando.set(false);
      },
    });

    this.startupService.certificacoes(startupId).subscribe({
      next: (lista) => this.certificacoes.set(lista),
      error: () => {}, // sem certificações, a seção só não aparece
    });

    const mentorId = this.sessao.mentorId();
    if (mentorId !== null) {
      this.conviteService.listarDoMentor(mentorId).subscribe({
        next: (lista) => this.convite.set(lista.find((c) => c.startupId === startupId && c.enviadoPor === 'MENTOR') ?? null),
        error: () => {},
      });
    }
  }

  voltar() {
    this.location.back();
  }

  convidar() {
    const mentorId = this.sessao.mentorId();
    const startupId = this.startup()?.id;
    if (mentorId === null || startupId == null || this.convite() || this.enviando()) return;

    this.enviando.set(true);
    this.erroConvite.set('');
    this.conviteService.criar({ startupId, mentorId, enviadoPor: 'MENTOR' }).subscribe({
      next: (convite) => {
        this.convite.set(convite);
        this.enviando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.enviando.set(false);
        this.erroConvite.set(
          e.status === 409
            ? 'Você já sugeriu uma mentoria para essa startup.'
            : mensagemDeErro(e, 'Não foi possível enviar a sugestão. Tente novamente.'),
        );
      },
    });
  }
}
