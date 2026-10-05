import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Startup } from '../models';
import { StartupService } from './startup.service';

@Component({
  selector: 'app-startup-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="card">
      <div class="cabecalho">
        <h1>Startups</h1>
        <a class="btn btn-primario" routerLink="/startups/novo">+ Nova startup</a>
      </div>

      @if (erro) {
        <p class="erro">{{ erro }}</p>
      }

      @if (carregando) {
        <p class="vazio">Carregando...</p>
      } @else if (startups.length === 0) {
        <p class="vazio">Nenhuma startup cadastrada ainda.</p>
      } @else {
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Segmento</th>
              <th>Cidade</th>
              <th>Maturidade</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (s of startups; track s.id) {
              <tr>
                <td>{{ s.nome }}</td>
                <td>{{ s.segmento }}</td>
                <td>{{ s.cidade }}{{ s.estado ? ' - ' + s.estado : '' }}</td>
                <td>{{ s.estagioMaturidade ?? '-' }}</td>
                <td class="acoes">
                  <a class="btn" [routerLink]="['/startups', s.id, 'editar']">Editar</a>
                  <button class="btn btn-perigo" (click)="excluir(s)">Excluir</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `,
})
export class StartupListComponent implements OnInit {
  startups: Startup[] = [];
  carregando = true;
  erro = '';

  constructor(private service: StartupService) {}

  ngOnInit() {
    this.carregar();
  }

  carregar() {
    this.service.listar().subscribe({
      next: (dados) => {
        this.startups = dados;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar. A API está rodando em localhost:8080?';
        this.carregando = false;
      },
    });
  }

  excluir(s: Startup) {
    if (!confirm(`Excluir a startup "${s.nome}"?`)) return;
    this.service.excluir(s.id!).subscribe({
      next: () => this.carregar(),
      error: () => (this.erro = 'Erro ao excluir.'),
    });
  }
}
