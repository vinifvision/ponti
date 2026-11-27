import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { mensagemDeErro } from '../../auth/mensagem-erro';
import { DialogoConfirmacaoComponent } from '../../shared/dialogo-confirmacao/dialogo-confirmacao.component';
import { IconeComponent } from '../../shared/icone/icone.component';
import { linkSeguro } from '../../shared/mentoria.utils';
import { SeloComponent } from '../../shared/selo/selo.component';
import { iniciais } from '../../shared/texto.utils';
import { AcompanhamentoService } from '../acompanhamento.service';
import { Anexo, Atividade, Entrega, PessoaAvatar } from '../acompanhamento.models';
import { ACCEPT_ARQUIVOS, MAX_ARQUIVOS, formatarTamanho, juntarArquivos } from '../anexos.utils';
import {
  TomNo, dataHoraCurta, entregueComAtraso, estaAtrasada, selosDaAtividade, textoPrazo, tomDoNo,
} from '../acompanhamento.utils';
import { ChipAnexoComponent } from './chip-anexo.component';
import { ComentariosEntregaComponent } from './comentarios-entrega.component';

const COR_NO: Record<TomNo, string> = {
  aprovada: 'bg-green-600 text-white',
  atrasada: 'bg-red-600 text-white',
  devolvida: 'bg-ponti-amarelo text-ponti-marinho',
  entregue: 'bg-ponti-roxo text-white',
  pendente: 'border-2 border-ponti-lilas bg-white text-ponti-marinho',
};

// Um passo da linha do tempo: bolinha numerada + cartão da atividade com a(s) entrega(s) da startup,
// a resposta do mentor (com o avatar dele) e a conversa. O que aparece de botão depende de quem vê e do estado.
// Com linhaDoTempo = false (abas Pendentes, Atrasadas e Devolvidas) mostra só o cartão, sem a bolinha nem a linha.
// Atividade que a startup ainda não entregou (pendente ou devolvida) mostra "Anexar link", "Anexar arquivo" e o
// botão de entregar; atividade já entregue mostra "Desfazer envio" enquanto o mentor não avaliar.
@Component({
  selector: 'app-passo-atividade',
  standalone: true,
  imports: [FormsModule, IconeComponent, SeloComponent, ComentariosEntregaComponent, ChipAnexoComponent, DialogoConfirmacaoComponent],
  host: { '[class]': 'classeHost()', role: 'listitem' },
  templateUrl: './passo-atividade.component.html',
})
export class PassoAtividadeComponent {
  atividade = input.required<Atividade>();
  numero = input.required<number>();
  ehMentor = input(false);
  somenteLeitura = input(false);
  nomeStartup = input('Startup');
  // A linha continua para baixo enquanto houver algo depois deste passo
  linhaAbaixo = input(true);
  // false = só o cartão da atividade (sem a bolinha numerada nem a linha do tempo)
  linhaDoTempo = input(true);
  // Quem aprova ou devolve: nome e foto para o avatar da resposta do mentor
  mentor = input<PessoaAvatar>({ nome: 'Mentor', fotoUrl: null });

  aprovar = output<Atividade>();
  devolver = output<Atividade>();
  atualizada = output<Atividade>();

  private service = inject(AcompanhamentoService);

  readonly iniciais = iniciais;
  readonly dataHoraCurta = dataHoraCurta;

  classeHost = computed(() =>
    this.linhaDoTempo() ? 'grid grid-cols-[3rem_minmax(0,1fr)] gap-x-5 sm:gap-x-8' : 'block',
  );
  // Os cartões da linha do tempo têm bastante espaço embaixo para a linha descer; na lista simples, só um respiro
  classeArtigo = computed(() => {
    const borda = this.atrasada() ? 'border-red-200' : 'border-gray-200';
    return `${borda} ${this.linhaDoTempo() ? 'mb-12 sm:mb-20' : 'mb-6'}`;
  });
  tom = computed(() => tomDoNo(this.atividade()));
  classeNo = computed(() => COR_NO[this.tom()]);
  classeLinha = computed(() => (this.atividade().status === 'APROVADA' ? 'bg-ponti-roxo' : 'bg-gray-200'));
  atrasada = computed(() => estaAtrasada(this.atividade()));
  selos = computed(() => selosDaAtividade(this.atividade()));
  prazo = computed(() => textoPrazo(this.atividade()));

  podeEntregar = computed(
    () => !this.somenteLeitura() && !this.ehMentor() && ['PENDENTE', 'DEVOLVIDA'].includes(this.atividade().status),
  );
  podeAvaliar = computed(() => !this.somenteLeitura() && this.ehMentor() && this.atividade().status === 'ENTREGUE');
  // A startup pode desfazer o envio enquanto o mentor ainda não avaliou
  podeDesfazer = computed(() => !this.somenteLeitura() && !this.ehMentor() && this.atividade().status === 'ENTREGUE');
  rotuloEntregar = computed(() => {
    if (this.atrasada()) return 'Enviar com atraso';
    return this.atividade().status === 'DEVOLVIDA' ? 'Reenviar entrega' : 'Entregar';
  });

  // Formulário de entrega (só a startup)
  formAberto = signal(false);
  texto = signal('');
  link = signal('');
  arquivos = signal<File[]>([]);
  enviando = signal(false);
  erro = signal('');

  readonly aceitar = ACCEPT_ARQUIVOS;
  readonly maximoArquivos = MAX_ARQUIVOS;
  readonly formatarTamanho = formatarTamanho;

  private entradaArquivo = viewChild.required<ElementRef<HTMLInputElement>>('entradaArquivo');
  private campoLink = viewChild<ElementRef<HTMLInputElement>>('campoLink');
  private dialogo = viewChild.required(DialogoConfirmacaoComponent);

  anexosDe(e: Entrega): Anexo[] {
    return e.anexos ?? [];
  }

  entregaAtrasada(e: Entrega): boolean {
    return entregueComAtraso(this.atividade(), e);
  }

  // Entregas antigas só foram substituídas porque o mentor devolveu; a última depende do status da atividade
  respostaDoMentor(indice: number): 'APROVADA' | 'DEVOLVIDA' | null {
    const a = this.atividade();
    if (indice < a.entregas.length - 1) return 'DEVOLVIDA';
    return a.status === 'APROVADA' || a.status === 'DEVOLVIDA' ? a.status : null;
  }

  linkSeguro(url: string | null): string | null {
    return linkSeguro(url);
  }

  rotuloLink(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
  }

  abrirForm() {
    this.erro.set('');
    this.formAberto.set(true);
  }

  cancelarForm() {
    this.formAberto.set(false);
    this.arquivos.set([]);
    this.erro.set('');
  }

  // "Anexar link": abre o formulário e leva o cursor para o campo do link
  anexarLink() {
    this.abrirForm();
    setTimeout(() => this.campoLink()?.nativeElement.focus());
  }

  // "Anexar arquivo": abre o formulário e a janela do computador para escolher os arquivos
  anexarArquivo() {
    this.abrirForm();
    this.entradaArquivo().nativeElement.click();
  }

  aoEscolherArquivos(evento: Event) {
    const entrada = evento.target as HTMLInputElement;
    const resultado = juntarArquivos(this.arquivos(), Array.from(entrada.files ?? []));
    this.arquivos.set(resultado.aceitos);
    this.erro.set(resultado.erro);
    entrada.value = ''; // permite escolher o mesmo arquivo de novo depois de removê-lo
  }

  removerArquivo(arquivo: File) {
    this.arquivos.update((lista) => lista.filter((a) => a !== arquivo));
    this.erro.set('');
  }

  async desfazerEnvio() {
    const confirmou = await this.dialogo().perguntar();
    if (!confirmou || this.enviando()) return;
    this.enviando.set(true);
    this.erro.set('');
    this.service.desfazerEntrega(this.atividade().id).subscribe({
      next: (atividade) => {
        this.enviando.set(false);
        this.atualizada.emit(atividade);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível desfazer o envio. Tente de novo.'));
        this.enviando.set(false);
      },
    });
  }

  enviar() {
    if (this.enviando()) return;
    const texto = this.texto().trim();
    const linkDigitado = this.link().trim();
    const link = linkDigitado ? linkSeguro(linkDigitado) : null;
    if (linkDigitado && !link) {
      this.erro.set('Informe um link válido, como https://exemplo.com.');
      return;
    }
    if (!texto && !link && !this.arquivos().length) {
      this.erro.set('Escreva um texto, informe um link ou anexe um arquivo para entregar.');
      return;
    }
    this.enviando.set(true);
    this.erro.set('');
    this.service.entregar(this.atividade().id, { texto, link, arquivos: this.arquivos() }).subscribe({
      next: (atividade) => {
        this.enviando.set(false);
        this.formAberto.set(false);
        this.texto.set('');
        this.link.set('');
        this.arquivos.set([]);
        this.atualizada.emit(atividade);
      },
      error: (e: HttpErrorResponse) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível enviar a entrega. Tente de novo.'));
        this.enviando.set(false);
      },
    });
  }
}
