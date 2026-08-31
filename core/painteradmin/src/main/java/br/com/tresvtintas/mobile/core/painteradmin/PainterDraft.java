package br.com.tresvtintas.mobile.core.painteradmin;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalLong;

public record PainterDraft(
        long organizationId,
        String name,
        String email,
        Optional<String> cpf,
        Optional<String> rg,
        Optional<String> phone,
        Optional<String> company,
        Optional<String> specialty,
        Optional<String> serviceArea,
        Optional<String> address,
        Optional<String> city,
        Optional<String> state,
        BigDecimal commissionRate,
        OptionalLong managerUserId,
        Optional<String> notes) {
    public PainterDraft {
        if (organizationId < 1 || email == null) {
            throw new IllegalArgumentException("Painter draft is invalid.");
        }
        name = PainterAdministrationModels.requireText(name, "Painter name");
        email = email.strip();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Painter draft is invalid.");
        }
        cpf = PainterAdministrationModels.optional(cpf);
        rg = PainterAdministrationModels.optional(rg);
        phone = PainterAdministrationModels.optional(phone);
        company = PainterAdministrationModels.optional(company);
        specialty = PainterAdministrationModels.optional(specialty);
        serviceArea = PainterAdministrationModels.optional(serviceArea);
        address = PainterAdministrationModels.optional(address);
        city = PainterAdministrationModels.optional(city);
        state = PainterAdministrationModels.optional(state)
                .map(String::toUpperCase);
        if (state.isPresent() && state.orElseThrow().length() != 2) {
            throw new IllegalArgumentException("Painter state is invalid.");
        }
        PainterAdministrationModels.requireRate(commissionRate);
        managerUserId = managerUserId == null
                ? OptionalLong.empty()
                : managerUserId;
        if (managerUserId.isPresent()
                && managerUserId.orElseThrow() < 1) {
            throw new IllegalArgumentException("Painter manager is invalid.");
        }
        notes = PainterAdministrationModels.optional(notes);
    }
}
