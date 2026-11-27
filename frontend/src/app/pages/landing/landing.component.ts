import { AfterViewInit, Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

type IconePasso = 'diagnostico' | 'mentores' | 'metas' | 'evolucao';

interface Passo {
  icone: IconePasso;
  titulo: string;
  texto: string;
}

interface Pergunta {
  pergunta: string;
  resposta: string;
}

interface LinkMenu {
  id: string;
  rotulo: string;
}

// Landing institucional da Ponti: primeira tela do site, antes do login/cadastro.
// Os estilos ficam todos em classes do Tailwind (o orçamento de CSS por componente do projeto é de 4 kB).
@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing.component.html',
})
export class LandingComponent implements AfterViewInit {
  private readonly destroyRef = inject(DestroyRef);

  readonly anoAtual = new Date().getFullYear();

  // Menu: o "id" é o id da seção na página
  readonly menu: LinkMenu[] = [
    { id: 'inicio', rotulo: 'Início' },
    { id: 'por-que-ponti', rotulo: 'Por que a Ponti?' },
    { id: 'como-funciona', rotulo: 'Como funciona' },
    { id: 'objetivo', rotulo: 'Nosso Objetivo' },
  ];

  readonly passos: Passo[] = [
    {
      icone: 'diagnostico',
      titulo: '01. Diagnóstico de maturidade (TRL NASA)',
      texto:
        'Calculamos o estágio real da sua startup usando a metodologia TRL da NASA (níveis 1 a 9), identificando exatamente onde o projeto está antes de traçar qualquer plano.',
    },
    {
      icone: 'mentores',
      titulo: '02. Conexão com mentores',
      texto:
        'Conectamos sua startup a mentores alinhados ao seu momento e desafio, formalizando o acompanhamento dentro da plataforma.',
    },
    {
      icone: 'metas',
      titulo: '03. Metas e atividades guiadas',
      texto:
        'Transforme orientações em metas claras e atividades com prazos visíveis, troca de materiais e acompanhamento contínuo entre mentor e equipe.',
    },
    {
      icone: 'evolucao',
      titulo: '04. Acompanhe e evolua de nível',
      texto:
        'Valide entregas concluídas, acompanhe o avanço do seu nível TRL e registre todo o histórico de evolução do projeto.',
    },
  ];

  readonly perguntas: Pergunta[] = [
    {
      pergunta: 'Quem pode usar a Ponti?',
      resposta:
        'Empreendedores, startups em qualquer estágio de maturidade que buscam orientação estruturada e mentores interessados em acompanhar projetos e compartilhar experiência prática de mercado.',
    },
    {
      pergunta: 'Como funciona o acompanhamento de metas e atividades?',
      resposta:
        'Após o diagnóstico inicial da startup e a conexão com o mentor, cada orientação vira metas claras e atividades com prazos definidos. Mentores e equipes acompanham entregas, anexos e o status de cada etapa em um só lugar, do início ao encerramento do ciclo.',
    },
    {
      pergunta: 'Preciso pagar para criar uma conta?',
      resposta:
        'Não. Criar sua conta, cadastrar seu projeto ou perfil de mentor e realizar o diagnóstico inicial são totalmente gratuitos. O investimento acontece apenas ao contratar as mentorias na plataforma.',
    },
  ];

  // TODO: a seção "Objetivo" ainda não tem conteúdo definido; são 3 espaços reservados (imagem/texto).
  readonly objetivos = [1, 2, 3];

  // Estado da tela
  readonly menuAberto = signal(false);
  readonly secaoAtiva = signal('inicio');
  readonly passoAberto = signal<number | null>(0);
  readonly perguntaAberta = signal<number | null>(0);

  ngAfterViewInit(): void {
    this.observarSecoes();
  }

  alternarPasso(indice: number): void {
    this.passoAberto.update((atual) => (atual === indice ? null : indice));
  }

  alternarPergunta(indice: number): void {
    this.perguntaAberta.update((atual) => (atual === indice ? null : indice));
  }

  // Rola suavemente até a seção (sem animação para quem prefere menos movimento)
  irPara(evento: Event, id: string): void {
    evento.preventDefault();
    this.menuAberto.set(false);
    const alvo = document.getElementById(id);
    if (!alvo) return;
    const reduzir = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    alvo.scrollIntoView({ behavior: reduzir ? 'auto' : 'smooth', block: 'start' });
    this.secaoAtiva.set(id);
  }

  // Destaca no menu a seção que está na faixa central da tela
  private observarSecoes(): void {
    if (typeof IntersectionObserver === 'undefined') return;

    const observador = new IntersectionObserver(
      (entradas) => {
        for (const entrada of entradas) {
          if (entrada.isIntersecting) this.secaoAtiva.set(entrada.target.id);
        }
      },
      { rootMargin: '-35% 0px -60% 0px' },
    );

    for (const item of this.menu) {
      const secao = document.getElementById(item.id);
      if (secao) observador.observe(secao);
    }
    this.destroyRef.onDestroy(() => observador.disconnect());
  }
}
