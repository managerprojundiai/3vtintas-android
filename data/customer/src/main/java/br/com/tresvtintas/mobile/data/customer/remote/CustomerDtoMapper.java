package br.com.tresvtintas.mobile.data.customer.remote;

import br.com.tresvtintas.mobile.core.customer.CustomerDetail;
import br.com.tresvtintas.mobile.core.customer.CustomerDraft;
import br.com.tresvtintas.mobile.core.customer.CustomerPage;
import br.com.tresvtintas.mobile.core.customer.CustomerSummary;
import br.com.tresvtintas.mobile.core.network.dto.CustomerCreateRequest;
import br.com.tresvtintas.mobile.core.network.dto.CustomerDetailDto;
import br.com.tresvtintas.mobile.core.network.dto.CustomerPageDto;
import br.com.tresvtintas.mobile.core.network.dto.CustomerSummaryDto;
import br.com.tresvtintas.mobile.core.network.dto.CustomerUpdateRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

final class CustomerDtoMapper {
    private CustomerDtoMapper() {
        throw new AssertionError("No instances.");
    }

    static CustomerPage toDomain(CustomerPageDto page) {
        return new CustomerPage(
                page.items().stream()
                        .map(CustomerDtoMapper::toDomain)
                        .toList(),
                Optional.ofNullable(page.nextCursor()));
    }

    static CustomerDetail toDomain(CustomerDetailDto customer) {
        return new CustomerDetail(
                customer.id(),
                optionalId(customer.userId()),
                optionalId(customer.organizationId()),
                customer.name(),
                Optional.ofNullable(customer.email()),
                Optional.ofNullable(customer.phone()),
                Optional.ofNullable(customer.cpf()),
                Optional.ofNullable(customer.address()),
                Optional.ofNullable(customer.city()),
                Optional.ofNullable(customer.state()),
                Optional.ofNullable(customer.notes()),
                optionalId(customer.assignedSalespersonUserId()),
                Instant.parse(customer.createdAt()),
                Instant.parse(customer.updatedAt()));
    }

    static CustomerCreateRequest createRequest(
            OptionalLong organizationId,
            CustomerDraft customer) {
        return new CustomerCreateRequest(
                optionalId(organizationId),
                customer.name(),
                customer.email().orElse(null),
                customer.phone().orElse(null),
                customer.cpf().orElse(null),
                customer.address().orElse(null),
                customer.city().orElse(null),
                customer.state().orElse(null),
                customer.notes().orElse(null));
    }

    static CustomerUpdateRequest updateRequest(CustomerDraft customer) {
        return new CustomerUpdateRequest(
                customer.name(),
                customer.email().orElse(null),
                customer.phone().orElse(null),
                customer.cpf().orElse(null),
                customer.address().orElse(null),
                customer.city().orElse(null),
                customer.state().orElse(null),
                customer.notes().orElse(null));
    }

    private static CustomerSummary toDomain(CustomerSummaryDto customer) {
        return new CustomerSummary(
                customer.id(),
                optionalId(customer.organizationId()),
                customer.name(),
                Optional.ofNullable(customer.email()),
                Optional.ofNullable(customer.phone()),
                Optional.ofNullable(customer.city()),
                Optional.ofNullable(customer.state()),
                optionalId(customer.assignedSalespersonUserId()),
                Instant.parse(customer.createdAt()),
                Instant.parse(customer.updatedAt()));
    }

    private static OptionalLong optionalId(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    private static Long optionalId(OptionalLong value) {
        return value.isPresent() ? value.getAsLong() : null;
    }
}
