import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PerfilLogadoService } from '../../auth/perfil-logado.service';
import { SessaoService } from '../../auth/sessao.service';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { MentorService } from '../../mentores/mentor.service';
import { Mentor, Startup } from '../../models';
import { NOMES_NIVEIS } from '../../shared/maturidade';
import { StartupService } from '../../startups/startup.service';
import { iniciais } from '../../shared/texto.utils';
import { EditorCertificacoesComponent } from '../../shared/editor-certificacoes/editor-certificacoes.component';

// "Editar perfil" (rota /perfil/editar): a startup ou o mentor edita os próprios dados.
// Quem chega aqui vem do botão "Editar perfil" da página do próprio perfil (/perfil).
// Salva com PATCH, mandando só os campos do formulário (o resto do perfil não é tocado).
@Component({
  selector: 'app-meu-perfil',
  standalone: true,
  imports: [FormsModule, RouterLink, EditorCertificacoesComponent],
  templateUrl: './meu-perfil.component.html',
})
export class MeuPerfilComponent implements OnInit {
  private sessao = inject(SessaoService);
  private mentorService = inject(MentorService);
  private startupService = inject(StartupService);
  private perfilLogado = inject(PerfilLogadoService);

  readonly niveis = Object.entries(NOMES_NIVEIS).map(([n, nome]) => ({ nivel: Number(n), nome }));
  readonly duracoes = [30, 45, 60, 90, 120];

  ehMentor = computed(() => this.sessao.mentorId() !== null);
  carregando = signal(true);
  salvando = signal(false);
  erro = signal('');
  sucesso = signal(false);

  mentor: Partial<Mentor> = {};
  startup: Partial<Startup> = {};
  gratuita = false;

  // Para o editor de certificações
  mentorId = this.sessao.mentorId();
  startupId = this.sessao.startupId();

  // O LinkedIn da startup só aparece quando a API já devolve esse campo (o backend ainda precisa criá-lo).
  // Antes disso, o campo salvaria "no vazio", então é melhor não mostrar.
  suportaLinkedin = signal(false);

  ngOnInit() {
    const mentorId = this.sessao.mentorId();
    const startupId = this.sessao.startupId();
    if (mentorId !== null) {
      this.mentorService.buscar(mentorId).subscribe({
        next: (m) => {
          this.mentor = { ...m };
          this.gratuita = Number(m.valorSessao ?? 0) === 0;
          this.carregando.set(false);
        },
        error: (e: HttpErrorResponse) => this.falhou(e),
      });
    } else if (startupId !== null) {
      this.startupService.buscar(startupId).subscribe({
        next: (s) => {
          this.startup = { ...s };
          this.suportaLinkedin.set('linkedinUrl' in s);
          this.carregando.set(false);
        },
        error: (e: HttpErrorResponse) => this.falhou(e),
      });
    }
  }

  foto(): string | null | undefined {
    return this.ehMentor() ? this.mentor.fotoUrl : this.startup.fotoUrl;
  }

  nome(): string {
    return (this.ehMentor() ? this.mentor.nome : this.startup.nome) ?? '';
  }

  iniciais(): string {
    return iniciais(this.nome());
  }

  salvar() {
    if (this.salvando()) return;
    if (this.nome().trim().length < 2) {
      this.erro.set('Informe o nome.');
      return;
    }
    this.erro.set('');
    this.sucesso.set(false);
    this.salvando.set(true);

    const mentorId = this.sessao.mentorId();
    const startupId = this.sessao.startupId();
    const pedido: Observable<{ nome: string; fotoUrl?: string | null }> =
      mentorId !== null
        ? this.mentorService.atualizarParcial(mentorId, {
            nome: this.mentor.nome,
            fotoUrl: this.mentor.fotoUrl ?? '',
            bio: this.mentor.bio ?? '',
            areasAtuacao: this.mentor.areasAtuacao ?? '',
            ecossistemas: this.mentor.ecossistemas ?? '',
            anosExperiencia: this.mentor.anosExperiencia ?? undefined,
            valorSessao: this.gratuita ? 0 : (this.mentor.valorSessao ?? 0),
            duracaoSessaoMinutos: this.mentor.duracaoSessaoMinutos ?? undefined,
            linkedinUrl: this.mentor.linkedinUrl ?? '',
            videoUrl: this.mentor.videoUrl ?? '',
          })
        : this.startupService.atualizarParcial(startupId!, {
            nome: this.startup.nome,
            fotoUrl: this.startup.fotoUrl ?? '',
            descricao: this.startup.descricao ?? '',
            segmento: this.startup.segmento ?? '',
            cidade: this.startup.cidade ?? '',
            estado: this.startup.estado ?? '',
            site: this.startup.site ?? '',
            videoPitchUrl: this.startup.videoPitchUrl ?? '',
            estagioMaturidade: this.startup.estagioMaturidade ?? undefined,
            ...(this.suportaLinkedin() ? { linkedinUrl: this.startup.linkedinUrl ?? '' } : {}),
          });

    pedido.subscribe({
      next: (perfil) => {
        this.salvando.set(false);
        this.sucesso.set(true);
        this.perfilLogado.atualizar(perfil.nome, perfil.fotoUrl ?? null);
        setTimeout(() => this.sucesso.set(false), 4000);
      },
      error: (e: HttpErrorResponse) => {
        this.salvando.set(false);
        this.erro.set(mensagemDeErro(e, 'Não foi possível salvar. Confira os campos.'));
      },
    });
  }

  private falhou(e: HttpErrorResponse) {
    this.erro.set(mensagemDeErro(e, 'Não foi possível carregar seu perfil.'));
    this.carregando.set(false);
  }
}
