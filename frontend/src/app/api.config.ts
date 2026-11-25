// Endereço do backend (Spring Boot), escolhido pelo endereço onde o site está aberto.
// A decisão acontece no navegador, então o mesmo build funciona em todos os lugares.

const host = window.location.hostname;

// API publicada no Render (usada pelo site na Vercel).
const API_PRODUCAO = 'https://ponti-api.onrender.com';

function escolherApi(): string {
  // Rodando no PC (ng serve)
  if (host === 'localhost' || host === '127.0.0.1') {
    return 'http://localhost:8080';
  }
  // No Codespaces o front fica em "...-4200.app.github.dev"
  // e a API em "...-8080.app.github.dev".
  if (host.endsWith('.app.github.dev')) {
    return `https://${host.replace('-4200.', '-8080.')}`;
  }
  // Qualquer outro endereço (Vercel)
  return API_PRODUCAO;
}

export const API_URL = escolherApi();
