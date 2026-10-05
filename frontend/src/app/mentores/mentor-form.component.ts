import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Mentor } from '../models';
import { MentorService } from './mentor.service';

@Component({
  selector: 'app-mentor-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card">
      <div class="cabecalho">
        <h1>{{ id ? 'Editar mentor' : 'Novo mentor' }}</h1>
      </div>

      <form #f="ngForm" (ngSubmit)="salvar(f.valid)">
        <label>
          Nome *
          <input name="nome" [(ngModel)]="mentor.nome" required />
          @if (f.submitted && !mentor.nome) {
            <span class="erro">O nome é obrigatório.</span>
          }
        </label>

        <label>
          Bio
          <textarea name="bio" rows="3" [(ngModel)]="mentor.bio"></textarea>
        </label>

        <label>
          Áreas de atuação
          <input name="areasAtuacao" [(ngModel)]="mentor.areasAtuacao" />
        </label>

        <label>
          Ecossistemas / comunidades
          <input name="ecossistemas" [(ngModel)]="mentor.ecossistemas" />
        </label>

        <label>
          Valor por sessão (R$)
          <input
            name="valorSessao"
            type="number"
            min="0"
            step="0.01"
            [(ngModel)]="mentor.valorSessao"
          />
        </label>

        @if (erro) {
          <span class="erro">{{ erro }}</span>
        }

        <div class="acoes">
          <button class="btn btn-primario" type="submit" [disabled]="salvando">Salvar</button>
          <a class="btn" routerLink="/mentores">Cancelar</a>
        </div>
      </form>
    </div>
  `,
})
export class MentorFormComponent implements OnInit {
  id: number | null = null;
  mentor: Mentor = { nome: '' };
  salvando = false;
  erro = '';

  constructor(
    private service: MentorService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id = Number(idParam);
      this.service.buscar(this.id).subscribe({
        next: (dados) => (this.mentor = dados),
        error: () => (this.erro = 'Mentor não encontrado.'),
      });
    }
  }

  salvar(valido: boolean | null) {
    if (!valido) return;
    this.salvando = true;
    this.erro = '';

    const dados = { ...this.mentor };
    if (dados.valorSessao === null || (dados.valorSessao as unknown) === '') {
      delete dados.valorSessao;
    }

    const requisicao = this.id
      ? this.service.atualizar(this.id, dados)
      : this.service.criar(dados);

    requisicao.subscribe({
      next: () => this.router.navigate(['/mentores']),
      error: () => {
        this.erro = 'Erro ao salvar. Confira os dados e se a API está no ar.';
        this.salvando = false;
      },
    });
  }
}
