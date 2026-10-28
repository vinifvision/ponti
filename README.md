<div align="center">

# Ponti

**Plataforma de Matchmaking entre Startups e Mentores**

[![Backend CI](https://github.com/vinifvision/ponti/actions/workflows/backend.yml/badge.svg)](https://github.com/vinifvision/ponti/actions/workflows/backend.yml)
[![Frontend CI](https://github.com/vinifvision/ponti/actions/workflows/frontend.yml/badge.svg)](https://github.com/vinifvision/ponti/actions/workflows/frontend.yml)
[![Status do Projeto](https://img.shields.io/badge/Status-Em_Desenvolvimento-yellow.svg)](#)
[![Licença](https://img.shields.io/badge/Licen%C3%A7a-MIT-blue.svg)](#)
[![Faculdade SENAC PE](https://img.shields.io/badge/Faculdade-SENAC_PE-004582.svg)](https://www.pe.senac.br/)

</div>

---

## 📌 Sobre o Projeto

O **Ponti** é uma solução digital desenvolvida como Projeto Integrador no curso de **Análise e Desenvolvimento de Sistemas** da **Faculdade SENAC Pernambuco**. 

A plataforma atua como um canal bidirecional e sem ruídos para o ecossistema de inovação, conectando **Startups** em estágio inicial (ideação e validação de MVP) a **Mentores** experientes. O sistema substitui as abordagens informais e dispersas por uma busca estruturada, combinando curadoria inteligente, privacidade (LGPD) e agilidade na tomada de decisão.

> ⚠️ **Nota do Repositório:** O projeto está em fase inicial de configuração e estruturação da sua arquitetura base. Novas atualizações e implementações do código-fonte serão publicadas em breve.

---

## 🎯 A Problemática e a Solução

- **O Problema:** Cerca de 1 em cada 4 startups fecha no primeiro ano de vida por falta de experiência. Hoje, encontrar um mentor depende de redes genéricas (como o LinkedIn) ou de eventos presenciais, sem uma ferramenta dedicada para filtrar o encaixe técnico dos envolvidos.
- **A Solução:** O Ponti centraliza a oferta e a busca de mentoria, num fluxo parecido com o do Preply. A startup encontra mentores com preço e calendário visíveis, escolhe um horário livre e paga (nesta versão, o pagamento é simulado). O mentor confirma enviando o link do Google Meet. O mentor também encontra startups e pode sugerir uma mentoria a elas. Nenhum contato pessoal (como WhatsApp) é exposto: a sessão acontece pelo link da reunião.

---

## 🛠️ Tecnologias e Arquitetura Planejadas

O projeto adota o modelo **Cliente-Servidor (API RESTful)** e uma arquitetura desacoplada em camadas:

* **Frontend (Single Page Application):** Angular
* **Backend (API RESTful):** Java (Spring Boot)
* **Banco de Dados:** PostgreSQL (Relacional com UUIDs)
* **Autenticação e Segurança:** JWT, Criptografia Bcrypt e conformidade com a LGPD.

---

## 📋 Funcionalidades Principais (MVP)

- [ ] **Gestão de Perfis Segregados:** Cadastro separado para fundadores de Startups e Mentores.
- [ ] **Busca Bidirecional Qualificada:** Filtros por área de expertise, estágio da startup e ecossistema de atuação.
- [x] **Agenda e Agendamento:** O mentor define os horários semanais; a startup agenda um horário livre e o mentor confirma com o link do Google Meet.
- [x] **Pagamento Simulado:** Pix ou cartão de demonstração, com reembolso simulado em recusas e cancelamentos.
- [ ] **Apresentação em Vídeo:** Suporte para inclusão do vídeo de pitch no perfil da startup.
- [ ] **Privacidade e Segurança:** Sem exposição de contato pessoal; as sessões acontecem pelo link da reunião.

---

## 📂 Estrutura das Entidades do Banco de Dados

* **`Usuario`**: Tabela base de autenticação, perfil e links profissionais.
* **`Startup`**: Dados do negócio, área de atuação, estágio e link do vídeo de pitch.
* **`Mentor`**: Áreas de expertise e ecossistemas dos quais participa.
* **`Conexao`**: Registra as solicitações e o status (*PENDENTE*, *ACEITA*, *RECUSADA*).

---

## Como rodar o projeto

O projeto é dividido em duas partes: `backend/` (API REST em Java com Spring Boot) e `frontend/` (SPA em Angular).

### Pré-requisitos

- Java 17 ou superior
- Maven (ou uma IDE com suporte a Maven)
- Node.js e npm

### Backend

```bash
cd backend
mvn spring-boot:run
```

- A API sobe em `http://localhost:8080`.
- A documentação interativa (Swagger UI) fica em `http://localhost:8080/swagger-ui.html`.
- A especificação OpenAPI está em [`openapi.yaml`](./openapi.yaml), na raiz do repositório.
- Por padrão o banco é o H2 (em memória), então os dados são apagados quando a aplicação reinicia. Para usar PostgreSQL, ajuste o `application.properties`: comente a configuração do H2 e descomente a do PostgreSQL.

### Frontend

Em outro terminal:

```bash
cd frontend
npm install
npx ng serve
```

A aplicação fica em `http://localhost:4200`. O backend precisa estar rodando.

### Rodando no GitHub Codespaces

- O front descobre sozinho o endereço da API (porta 8080 do mesmo Codespace).
- A porta **8080** precisa estar com visibilidade **Public** na aba *Ports*. Ela volta a ficar privada sempre que o backend reinicia. Para torná-la pública de novo:

```bash
gh codespace ports visibility 8080:public -c $CODESPACE_NAME
```

## Endpoints

Todas as rotas, menos login e cadastro, exigem o cabeçalho `Authorization: Bearer <token>`.

| Recurso | Rota | Operações |
|---|---|---|
| Startups | `/startups` | listar, buscar por id, `PATCH` (edição parcial do próprio perfil) e excluir a própria conta (a criação é pelo cadastro) |
| Mentores | `/mentores` | listar e buscar já com a reputação (nota média, avaliações e mentorias realizadas), `PATCH` do próprio perfil e excluir a própria conta |
| Avaliações | `POST /agendamentos/{id}/avaliacao`, `GET /mentores/{id}/avaliacoes` | a startup dá nota de 1 a 5 depois da mentoria |
| Disponibilidade do mentor | `GET` e `PUT /mentores/{id}/disponibilidade` | horários em que o mentor atende na semana |
| Horários livres | `GET /mentores/{id}/horarios?de=2026-11-01&ate=2026-11-30` | blocos livres para agendar |
| Agendamentos | `GET /agendamentos` (as mentorias de quem está logado), `GET /agendamentos/{id}`, `POST /agendamentos` | lista, acompanha e cria mentorias com pagamento (simulado) |
| Responder agendamento | `POST /agendamentos/{id}/confirmar` (com o link do Meet), `/recusar`, `/cancelar` | ciclo de vida da mentoria |
| Sugestões de mentoria | `POST /convites`, `GET /convites/detalhados`, `/convites/{id}/aceitar` e `/recusar` | o mentor sugere uma mentoria a uma startup |
| Cadastro | `POST /auth/cadastro/startup`, `POST /auth/cadastro/mentor` | cria a conta (e-mail + senha com BCrypt) e o perfil juntos |
| Login | `POST /auth/login` | confere e-mail e senha e devolve um token JWT (válido por 8h) |
| Quem está logado | `GET /auth/me` com `Authorization: Bearer <token>` | devolve o usuário dono do token |

## Pagamento (simulado)

Nenhuma cobrança real é feita, mas o fluxo imita um gateway de verdade (classe `GatewayPagamentoSimulado`):

- **Pix:** a API gera o código "copia e cola" no padrão **BR Code do Banco Central** (com CRC16), o front mostra o QR Code e, alguns segundos depois, o "banco" confirma sozinho (uma tarefa agendada faz o papel do webhook). Pix não pago em 15 minutos expira e libera o horário.
- **Cartão:** valida número (algoritmo de Luhn), validade e CVV, descobre a bandeira e aprova na hora. Para testar a recusa, use um cartão terminado em **0002** (ex.: `4000 0000 0000 0002`). Do cartão, a API guarda só a bandeira e os 4 últimos dígitos.
- O mentor só recebe o pedido depois que o pagamento é aprovado. Se ele recusar ou alguém cancelar, o valor aparece como reembolsado.
- Cada pagamento tem um código de transação (ex.: `PNT-7K2M9QX4`), mostrado no comprovante.

Para trocar por um gateway real (Mercado Pago, Stripe...), basta reimplementar essa classe.

## Segurança

- **Login com JWT:** `POST /auth/login` devolve um token assinado (válido por 8h). O filtro `JwtAutenticacaoFilter` lê o token em cada requisição e identifica o usuário. Senhas são guardadas com BCrypt.
- **Cada um só mexe no que é seu:** a classe `Acesso` confere o dono antes de cada alteração. Uma startup não agenda em nome de outra, só o mentor do pedido confirma ou recusa, só quem participa cancela, e ninguém edita ou exclui o perfil de outra pessoa.
- **Respostas:** sem token (ou token inválido) → **401**; com token, mas sem permissão → **403**.
- **Excluir a conta** apaga junto o login, a agenda, os convites e os agendamentos (nada fica órfão).
- **CORS** liberado só para as origens de `ponti.cors.origens` (por padrão o front local e o Codespaces).

## Formato dos erros

Todos os erros seguem o padrão **Problem Details** (RFC 9457), sempre em português:

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "email: deve ser um endereço de e-mail bem formado",
  "instance": "/auth/cadastro/mentor",
  "message": "email: deve ser um endereço de e-mail bem formado",
  "campos": [
    { "campo": "email", "mensagem": "deve ser um endereço de e-mail bem formado" }
  ]
}
```

O campo `message` é o texto pronto para mostrar na tela. `campos` só aparece em erros de validação.

## Contas de exemplo

Ao subir o backend com o banco vazio, ficam disponíveis estas contas (senha **ponti1234**):

| E-mail | Tipo |
|---|---|
| `petcare@exemplo.com` | Startup (PetCare Digital) |
| `saudeagora@exemplo.com` | Startup (SaúdeAgora) |
| `juliana@exemplo.com` | Mentor (Juliana G.) |
| `roberto@exemplo.com` | Mentor (Roberto) |

Os mentores de exemplo já têm horários cadastrados: a Juliana atende às terças e quintas de manhã (e quinta à tarde), e o Roberto às segundas e quartas à tarde e sexta de manhã.

Além dessas, o backend carrega **dados de demonstração** (`DadosDemonstracao`): 12 mentores de áreas diferentes, 6 startups e cerca de 30 mentorias (realizadas e avaliadas, confirmadas e esperando resposta). Assim a plataforma já abre "em uso". Os logins extras seguem o padrão `nome@exemplo.com` (ex.: `camila@`, `rafael@`, `thiago@`, `finfacil@`, `agrosense@`), todos com a senha **ponti1234**. Para começar com poucos dados, rode com `PONTI_DEMO=false`.

## Testes

```bash
cd backend
mvn test
```

São quase 150 testes automatizados (controllers, regras de agenda, pagamento Pix e cartão, avaliações, convites, cadastro, login e segurança com token JWT real). Eles rodam sozinhos no **GitHub Actions** a cada push e pull request que mexe no backend (`.github/workflows/backend.yml`).

## Tecnologias

- **Backend:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Bean Validation, springdoc-openapi, H2 / PostgreSQL
- **Frontend:** Angular, TypeScript
- **Documentação da API:** OpenAPI 3.0 (Swagger em `/swagger-ui.html`)
- **Integração contínua:** GitHub Actions

---

## 👥 Equipe Desenvolvedora

<table align="center">
  <tr>
    <td align="center">
      <a href="https://github.com/vinifvision">
        <img src="https://github.com/vinifvision.png" width="100px;" alt="Vinícius Fernandes"/><br />
        <sub><b>Vinícius Fernandes</b></sub>
      </a><br />
      <sub>Backend & DevOps</sub>
    </td>
    <td align="center">
      <a href="https://github.com/pedrocaribe06">
        <img src="https://github.com/pedrocaribe06.png" width="100px;" alt="Pedro Enrico"/><br />
        <sub><b>Pedro Enrico</b></sub>
      </a><br />
      <sub>Frontend & UI/UX</sub>
    </td>
    <td align="center">
      <a href="https://github.com/PedroM8800">
        <img src="https://github.com/PedroM8800.png" width="100px;" alt="Pedro Moura"/><br />
        <sub><b>Pedro Moura</b></sub>
      </a><br />
      <sub>Backend & Modelagem</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="#">
        <img src="https://ui-avatars.com/api/?name=Sam+Diego&background=0D8ABC&color=fff" width="100px;" alt="Sam Diego"/><br />
        <sub><b>Sam Diego</b></sub>
      </a><br />
      <sub>Backend & Modelagem</sub>
    </td>
    <td align="center">
      <a href="https://github.com/laags6">
        <img src="https://github.com/laags6.png" width="100px;" alt="Larissa Beatriz"/><br />
        <sub><b>Larissa Beatriz</b></sub>
      </a><br />
      <sub>Frontend & UI/UX</sub>
    </td>
  </tr>
</table>
