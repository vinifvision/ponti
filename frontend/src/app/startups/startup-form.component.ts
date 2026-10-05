import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Startup } from '../models';
import { StartupService } from './startup.service';

@Component({
  selector: 'app-startup-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card">
      <div class="cabecalho">
        <h1>{{ id ? 'Editar startup' : 'Nova startup' }}</h1>
      </div>

      <form #f="ngForm" (ngSubmit)="salvar(f.valid)">
        <label>
          Nome *
          <input name="nome" [(ngModel)]="startup.nome" required />
          @if (f.submitted && !startup.nome) {
            <span class="erro">O nome é obrigatório.</span>
          }
        </label>

        <label>
          Descrição
          <textarea name="descricao" rows="3" [(ngModel)]="startup.descricao"></textarea>
        </label>

        <label>
          Vídeo de pitch (URL)
          <input name="videoPitchUrl" [(ngModel)]="startup.videoPitchUrl" />
        </label>

        <label>
          Atuações
          <textarea name="atuacoes" rows="2" [(ngModel)]="startup.atuacoes"></textarea>
        </label>

        <div class="grid2">
          <label>
            Segmento
            <input name="segmento" [(ngModel)]="startup.segmento" />
          </label>
          <label>
            Site
            <input name="site" [(ngModel)]="startup.site" />
          </label>
          <label>
            Cidade
            <input name="cidade" [(ngModel)]="startup.cidade" />
          </label>
          <label>
            Estado (UF)
            <input name="estado" maxlength="2" [(ngModel)]="startup.estado" />
          </label>
        </div>

        <label>
          Estágio de maturidade (1 a 9)
          <input
            name="estagioMaturidade"
            type="number"
            min="1"
            max="9"
            [(ngModel)]="startup.estagioMaturidade"
          />
        </label>

        @if (erro) {
          <span class="erro">{{ erro }}</span>
        }

        <div class="acoes">
          <button class="btn btn-primario" type="submit" [disabled]="salvando">Salvar</button>
          <a class="btn" routerLink="/startups">Cancelar</a>
        </div>
      </form>
    </div>
  `,
})
export class StartupFormComponent implements OnInit {
  id: number | null = null;
  startup: Startup = { nome: '' };
  salvando = false;
  erro = '';

  constructor(
    private service: StartupService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id = Number(idParam);
      this.service.buscar(this.id).subscribe({
        next: (dados) => (this.startup = dados),
        error: () => (this.erro = 'Startup não encontrada.'),
      });
    }
  }

  salvar(valido: boolean | null) {
    if (!valido) return;
    this.salvando = true;
    this.erro = '';

    const dados = { ...this.startup };
    if (dados.estagioMaturidade === null || (dados.estagioMaturidade as unknown) === '') {
      delete dados.estagioMaturidade;
    }

    const requisicao = this.id
      ? this.service.atualizar(this.id, dados)
      : this.service.criar(dados);

    requisicao.subscribe({
      next: () => this.router.navigate(['/startups']),
      error: () => {
        this.erro = 'Erro ao salvar. Confira os dados e se a API está no ar.';
        this.salvando = false;
      },
    });
  }
}
