package com.ponti.api.model;

// ADMIN: conta da equipe da Ponti, criada a partir das variáveis de ambiente (ver ContaAdmin).
// Não tem perfil de startup nem de mentor e só usa as rotas /admin.
public enum TipoUsuario {
    STARTUP, MENTOR, ADMIN
}
