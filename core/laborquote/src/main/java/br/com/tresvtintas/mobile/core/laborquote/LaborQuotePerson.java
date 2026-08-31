package br.com.tresvtintas.mobile.core.laborquote;

public record LaborQuotePerson(long id, String name) {
    public LaborQuotePerson {
        if (id < 1 || name == null || name.isBlank() || name.length() > 200) {
            throw new IllegalArgumentException("Labor quote person is invalid.");
        }
    }
}
