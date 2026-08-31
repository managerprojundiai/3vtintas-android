package br.com.tresvtintas.mobile.core.customer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.util.Optional;
import org.junit.Test;

public final class CustomerDomainTest {
    @Test
    public void normalizesSearchAndCustomerDraft() {
        CustomerQuery query = CustomerQuery.initial().withSearch(
                "  Cliente   Centro ");
        CustomerDraft draft = CustomerDraft.fromRaw(
                "  Maria   Souza ",
                " maria@example.test ",
                " 11999999999 ",
                "",
                " Rua A, 10 ",
                " São Paulo ",
                "sp",
                " Visitar à tarde ");

        assertEquals(
                "Search must be canonical before transport.",
                "cliente centro",
                query.search().orElseThrow());
        assertEquals(
                "Name whitespace must be canonical.",
                "Maria Souza",
                draft.name());
        assertEquals(
                "State must be uppercase.",
                "SP",
                draft.state().orElseThrow());
        assertFalse(
                "Blank CPF must become an explicit empty optional.",
                draft.cpf().isPresent());
    }

    @Test
    public void rejectsMalformedEmailAndOversizedSearch() {
        assertThrows(
                "Invalid e-mail must fail before network transport.",
                IllegalArgumentException.class,
                () -> CustomerDraft.fromRaw(
                        "Cliente",
                        "invalid",
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""));
        assertThrows(
                "Search longer than the public contract must fail.",
                IllegalArgumentException.class,
                () -> new CustomerQuery(Optional.of("x".repeat(81)), 30));
    }
}
