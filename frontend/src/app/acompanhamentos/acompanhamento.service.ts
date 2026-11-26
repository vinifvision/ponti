import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, defer, delay, of, throwError } from 'rxjs';
import { API_URL } from '../api.config';
import { SessaoService } from '../auth/sessao.service';
import { BancoMock, ErroMock } from './acompanhamento.mock';
import {
  Atividade,
  Meta,
  NovaAtividade,
  NovaEntrega,
  NovaProposta,
  PainelConsolidado,
  ResumoAcompanhamento,
} from './acompanhamento.models';

@Injectable({ providedIn: 'root' })
export class AcompanhamentoService {
  private http = inject(HttpClient);
  private sessao = inject(SessaoService);

  private readonly usarMock = false;
  private banco: BancoMock | null = null;
  private bancoDe: number | null = null;

  // ----- lista e painel (Tela 1) -----

  // GET /acompanhamentos -> os acompanhamentos de quem está logado
  listar(): Observable<ResumoAcompanhamento[]> {
    return this.usarMock
      ? this.simular((b) => b.listar())
      : this.http.get<ResumoAcompanhamento[]>(`${API_URL}/acompanhamentos`);
  }

  // GET /acompanhamentos/painel -> painel consolidado da startup
  painel(): Observable<PainelConsolidado> {
    return this.usarMock
      ? this.simular((b) => b.painel())
      : this.http.get<PainelConsolidado>(`${API_URL}/acompanhamentos/painel`);
  }

  // POST /acompanhamentos/{id}/aceitar e /recusar -> a startup responde à proposta
  aceitar(id: number): Observable<void> {
    return this.usarMock
      ? this.simular((b) => b.aceitar(id))
      : this.http.post<void>(`${API_URL}/acompanhamentos/${id}/aceitar`, {});
  }

  recusar(id: number): Observable<void> {
    return this.usarMock
      ? this.simular((b) => b.recusar(id))
      : this.http.post<void>(`${API_URL}/acompanhamentos/${id}/recusar`, {});
  }

  // POST /acompanhamentos -> o mentor propõe (objetivo + metas iniciais opcionais)
  propor(nova: NovaProposta): Observable<ResumoAcompanhamento> {
    if (this.usarMock) return this.simular((b) => b.propor(nova));
    const { nomeStartup, ...corpo } = nova;
    void nomeStartup;
    return this.http.post<ResumoAcompanhamento>(`${API_URL}/acompanhamentos`, corpo);
  }

  // ----- página do acompanhamento (Tela 3) -----

  // GET /acompanhamentos/{id}
  detalhe(id: number): Observable<ResumoAcompanhamento> {
    return this.usarMock
      ? this.simular((b) => b.detalhe(id))
      : this.http.get<ResumoAcompanhamento>(`${API_URL}/acompanhamentos/${id}`);
  }

  // POST /acompanhamentos/{id}/encerrar (só o mentor)
  encerrar(id: number): Observable<ResumoAcompanhamento> {
    return this.usarMock
      ? this.simular((b) => b.encerrar(id))
      : this.http.post<ResumoAcompanhamento>(`${API_URL}/acompanhamentos/${id}/encerrar`, {});
  }

  // GET /acompanhamentos/{id}/metas
  metas(id: number): Observable<Meta[]> {
    return this.usarMock
      ? this.simular((b) => b.metas(id))
      : this.http.get<Meta[]>(`${API_URL}/acompanhamentos/${id}/metas`);
  }

  // PATCH /metas/{id} (só o mentor marca/desmarca)
  atualizarMeta(metaId: number, cumprida: boolean): Observable<Meta> {
    return this.usarMock
      ? this.simular((b) => b.atualizarMeta(metaId, cumprida))
      : this.http.patch<Meta>(`${API_URL}/metas/${metaId}`, { cumprida });
  }

  // GET /acompanhamentos/{id}/atividades
  atividades(id: number): Observable<Atividade[]> {
    return this.usarMock
      ? this.simular((b) => b.atividades(id))
      : this.http.get<Atividade[]>(`${API_URL}/acompanhamentos/${id}/atividades`);
  }

  // POST /acompanhamentos/{id}/atividades (mentor)
  // Com arquivos anexados vai como multipart/form-data (campos titulo, descricao, prazo e arquivos);
  // sem arquivos, como JSON.
  criarAtividade(acompanhamentoId: number, dados: NovaAtividade): Observable<Atividade> {
    if (this.usarMock) return this.simular((b) => b.criarAtividade(acompanhamentoId, dados));
    const { arquivos, ...campos } = dados;
    return this.http.post<Atividade>(
      `${API_URL}/acompanhamentos/${acompanhamentoId}/atividades`,
      arquivos?.length ? this.comArquivos(campos, arquivos) : campos,
    );
  }

  // POST /atividades/{id}/entrega (startup: envia ou reenvia)
  // Com arquivos vai como multipart/form-data (campos texto, link e arquivos); sem arquivos, como JSON.
  entregar(atividadeId: number, dados: NovaEntrega): Observable<Atividade> {
    if (this.usarMock) return this.simular((b) => b.entregar(atividadeId, dados));
    const { arquivos, ...campos } = dados;
    return this.http.post<Atividade>(
      `${API_URL}/atividades/${atividadeId}/entrega`,
      arquivos?.length ? this.comArquivos(campos, arquivos) : campos,
    );
  }

  // DELETE /atividades/{id}/entrega (startup: desfaz o último envio, só enquanto o mentor não avaliou)
  desfazerEntrega(atividadeId: number): Observable<Atividade> {
    return this.usarMock
      ? this.simular((b) => b.desfazerEntrega(atividadeId))
      : this.http.delete<Atividade>(`${API_URL}/atividades/${atividadeId}/entrega`);
  }

  // POST /atividades/{id}/aprovar (mentor, feedback opcional)
  aprovar(atividadeId: number, feedback?: string): Observable<Atividade> {
    return this.usarMock
      ? this.simular((b) => b.aprovar(atividadeId, feedback))
      : this.http.post<Atividade>(`${API_URL}/atividades/${atividadeId}/aprovar`, { feedback });
  }

  // POST /atividades/{id}/devolver (mentor, feedback obrigatório)
  devolver(atividadeId: number, feedback: string): Observable<Atividade> {
    return this.usarMock
      ? this.simular((b) => b.devolver(atividadeId, feedback))
      : this.http.post<Atividade>(`${API_URL}/atividades/${atividadeId}/devolver`, { feedback });
  }

  // POST /atividades/{id}/entregas/{entregaId}/comentarios (os dois lados)
  // Ainda não existe na especificação: combinar o endereço com o backend.
  comentar(atividadeId: number, entregaId: number, texto: string): Observable<Atividade> {
    return this.usarMock
      ? this.simular((b) => b.comentar(atividadeId, entregaId, texto))
      : this.http.post<Atividade>(`${API_URL}/atividades/${atividadeId}/entregas/${entregaId}/comentarios`, { texto });
  }

  private comArquivos(campos: Record<string, string | null | undefined>, arquivos: File[]): FormData {
    const corpo = new FormData();
    for (const [nome, valor] of Object.entries(campos)) {
      if (valor !== null && valor !== undefined) corpo.append(nome, valor);
    }
    arquivos.forEach((a) => corpo.append('arquivos', a, a.name));
    return corpo;
  }

  // ----- dados de mentira (apagar junto com o mock) -----

  // Cada pessoa logada tem o seu banco; trocar de usuário começa de novo com os dados de exemplo
  private bancoAtual(): BancoMock {
    const id = this.sessao.usuario()?.id ?? 0;
    if (!this.banco || this.bancoDe !== id) {
      const tipo = this.sessao.tipo() === 'MENTOR' ? 'MENTOR' : 'STARTUP';
      this.banco = new BancoMock(tipo, this.sessao.nome() ?? (tipo === 'MENTOR' ? 'Mentor' : 'Sua startup'));
      this.bancoDe = id;
    }
    return this.banco;
  }

  private simular<T>(acao: (banco: BancoMock) => T): Observable<T> {
    return defer(() => {
      try {
        return of(acao(this.bancoAtual()));
      } catch (e) {
        if (e instanceof ErroMock) {
          return throwError(
            () => new HttpErrorResponse({ status: e.status, statusText: e.message, error: { message: e.message } }),
          );
        }
        return throwError(() => e);
      }
    }).pipe(delay(300));
  }
}
