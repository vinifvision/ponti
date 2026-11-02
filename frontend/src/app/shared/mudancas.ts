// "Foto" do que está no formulário, para saber se a pessoa mexeu em algo depois de carregar ou salvar.
// Campo vazio ('' ou null) vale o mesmo que campo ausente: digitar e apagar em seguida não conta como mudança.
export function fotografar(valor: unknown): string {
  return JSON.stringify(valor, (_chave, v) => (v === '' || v === null ? undefined : v));
}
