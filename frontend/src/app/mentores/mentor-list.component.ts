import { CurrencyPipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Mentor } from '../models';
import { MentorService } from './mentor.service';

@Component({
  selector: 'app-mentor-list',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  template: `
    <div class="card">
      <div class="cabecalho">
        <h1>Mentores</h1>
        <a class="btn btn-primario" routerLink="/mentores/novo">+ Novo mentor</a>
      </div>

      @if (erro) {
        <p class="erro">{{ erro }}</p>
      }

      @if (carregando) {
        <p class="vazio">Carregando...</p>
      } @else if (mentores.length === 0) {
        <p class="vazio">Nenhum mentor cadastrado ainda.</p>
      } @else {
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Áreas de atuação</th>
              <th>Ecossistemas</th>
              <th>Valor/sessão</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (m of mentores; track m.id) {
              <tr>
                <td>{{ m.nome }}</td>
                <td>{{ m.areasAtuacao }}</td>
                <td>{{ m.ecossistemas }}</td>
                <td>{{ m.valorSessao != null ? (m.valorSessao | currency: 'BRL') : '-' }}</td>
                <td class="acoes">
                  <a class="btn" [routerLink]="['/mentores', m.id, 'editar']">Editar</a>
                  <button class="btn btn-perigo" (click)="excluir(m)">Excluir</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `,
})
export class MentorListComponent implements OnInit {
  mentores: Mentor[] = [];
  carregando = true;
  erro = '';

  constructor(private service: MentorService) {}

  ngOnInit() {
    this.carregar();
  }

  carregar() {
    this.service.listar().subscribe({
      next: (dados) => {
        this.mentores = dados;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar. A API está rodando em localhost:8080?';
        this.carregando = false;
      },
    });
  }

  excluir(m: Mentor) {
    if (!confirm(`Excluir o mentor "${m.nome}"?`)) return;
    this.service.excluir(m.id!).subscribe({
      next: () => this.carregar(),
      error: () => (this.erro = 'Erro ao excluir.'),
    });
  }
}
