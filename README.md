<div align="center">

# Ponti

**Plataforma de Matchmaking entre Startups e Mentores**

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
- **A Solução:** O Ponti centraliza a oferta e busca de mentoria. As startups apresentam seu negócio e vídeo de pitch para mentores específicos; os mentores recebem convites organizados em um *inbox*, aceitando ou recusando conexões em poucos cliques, sem expor dados pessoais antes do aceite mútuo.

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
- [ ] **Inbox de Conexões:** Painel para o mentor gerenciar (aceitar/recusar) solicitações de mentoria.
- [ ] **Apresentação em Vídeo:** Suporte para inclusão do vídeo de pitch no perfil da startup.
- [ ] **Privacidade e Segurança:** Dados de contato direto ofuscados até a confirmação do *match*.

---

## 📂 Estrutura das Entidades do Banco de Dados

* **`Usuario`**: Tabela base de autenticação, perfil e links profissionais.
* **`Startup`**: Dados do negócio, área de atuação, estágio e link do vídeo de pitch.
* **`Mentor`**: Áreas de expertise e ecossistemas dos quais participa.
* **`Conexao`**: Registra as solicitações e o status (*PENDENTE*, *ACEITA*, *RECUSADA*).

---

## 🚀 Como Executar o Projeto Localmente

*(As instruções abaixo serão atualizadas assim que as dependências do código base forem commitadas)*

### Pré-requisitos
* [Git](https://git-scm.com/)
* [Node.js](https://nodejs.org/) (Versão LTS recomendada)
* [PostgreSQL](https://www.postgresql.org/)

### Passos de Instalação

1. **Clone o repositório:**
```bash
git clone [https://github.com/vinifvision/ponti.git](https://github.com/vinifvision/ponti.git)
cd ponti
```

2. **Instale as dependências:**
```bash
npm install
```

3. **Configure as Variáveis de Ambiente:**

Crie um arquivo `.env` na raiz do projeto com base no arquivo `.env.example`.

5. **Execute as Migrações do Banco de Dados:**
```bash
npm run migrate
```

5. **Inicie o servidor de desenvolvimento:**
```bash
npm run dev
```

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
