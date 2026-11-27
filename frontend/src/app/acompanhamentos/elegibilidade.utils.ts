import { Agendamento } from '../models';
import { ResumoAcompanhamento } from './acompanhamento.models';

export interface StartupElegivel {
  id: number;
  nome: string;
  descricao: string | null; // segmento da startup
  fotoUrl: string | null;
  mentoriasRealizadas: number;
  ultimaMentoria: string; // início da mentoria mais recente (como veio da API)
}

// Startups para as quais o mentor pode propor um acompanhamento: as que já tiveram ao menos uma mentoria
// realizada com ele e que ainda não têm proposta pendente nem acompanhamento ativo com ele.
// A mais recente primeiro. (O backend precisa aplicar a mesma regra ao criar a proposta.)
export function startupsElegiveis(agendamentos: Agendamento[], acompanhamentos: ResumoAcompanhamento[]): StartupElegivel[] {
  const ocupadas = new Set(
    acompanhamentos.filter((a) => a.status === 'PROPOSTO' || a.status === 'ATIVO').map((a) => a.parceiro.id),
  );
  const porStartup = new Map<number, StartupElegivel>();

  for (const a of agendamentos) {
    if (!a.realizada || !a.startup || ocupadas.has(a.startup.id)) continue;
    const atual = porStartup.get(a.startup.id);
    if (!atual) {
      porStartup.set(a.startup.id, {
        id: a.startup.id,
        nome: a.startup.nome,
        descricao: a.startup.descricao ?? null,
        fotoUrl: a.startup.fotoUrl ?? null,
        mentoriasRealizadas: 1,
        ultimaMentoria: a.inicio,
      });
    } else {
      atual.mentoriasRealizadas++;
      if (a.inicio > atual.ultimaMentoria) atual.ultimaMentoria = a.inicio;
    }
  }
  return [...porStartup.values()].sort((x, y) => y.ultimaMentoria.localeCompare(x.ultimaMentoria));
}
