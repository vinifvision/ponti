// Endereço do backend (Spring Boot). Troque aqui se a API estiver em outro lugar.
// export const API_URL = 'http://localhost:8080';

const host = window.location.hostname;

// No Codespaces o front fica em "...-4200.app.github.dev"
// e a API em "...-8080.app.github.dev".
export const API_URL = host.endsWith('.app.github.dev')
  ? `https://${host.replace('-4200.', '-8080.')}`
  : 'http://localhost:8080';
