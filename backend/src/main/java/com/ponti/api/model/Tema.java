package com.ponti.api.model;

// Assuntos em que uma startup pode precisar de ajuda e em que um mentor pode ajudar.
// É a lista que os dois lados compartilham: o "match" compara os temas da startup com os do mentor.
// O nome do enum (VALIDACAO, PITCH...) é o que trafega na API; o rótulo é o que aparece na tela.
public enum Tema {
    VALIDACAO("Validação de ideia"),
    PRODUTO("Produto e UX"),
    TECNOLOGIA("Tecnologia e desenvolvimento"),
    PITCH("Pitch e captação"),
    FINANCAS("Finanças e precificação"),
    VENDAS("Vendas e primeiros clientes"),
    MARKETING("Marketing e marca"),
    JURIDICO("Jurídico e contratos"),
    GESTAO("Gestão e time");

    private final String rotulo;

    Tema(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
