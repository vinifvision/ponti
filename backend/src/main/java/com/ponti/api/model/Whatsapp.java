package com.ponti.api.model;

// Regras do número de WhatsApp (só números do Brasil).
// Gravamos sempre no mesmo formato: 55 + DDD + número, só dígitos (ex.: "5581999998888").
// Assim, "(81) 99999-8888", "81999998888" e "+55 81 99999-8888" viram o mesmo valor
// e o link https://wa.me/5581999998888 funciona direto.
public final class Whatsapp {

    // 55 + DDD (2 dígitos) + número fixo (8) ou celular (9)
    public static final String FORMATO = "55\\d{10,11}";

    private Whatsapp() {
    }

    // Tira tudo que não é dígito e coloca o 55 na frente quando a pessoa digitou só DDD + número.
    // Vazio vira null (campo não informado). Se o número for inválido, devolve algo fora do
    // FORMATO e a validação (@Pattern) recusa: "abc" não pode virar "não informado".
    public static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.isEmpty()) return valor.trim();
        if (digitos.length() == 10 || digitos.length() == 11) return "55" + digitos;
        return digitos;
    }
}
