package com.ponti.api.dto;

// Resposta de GET /mentores/{id}/contato e /startups/{id}/contato: só o próprio dono vê.
// Usado na tela "Editar perfil" para carregar o WhatsApp e a autorização de compartilhar.
public record ContatoPrivado(String whatsapp, boolean compartilharWhatsapp) {
}
