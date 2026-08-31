package br.com.tresvtintas.mobile.core.useradmin;

import br.com.tresvtintas.mobile.core.model.AppRole;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Mutation;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Options;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.Page;
import br.com.tresvtintas.mobile.core.useradmin.UserAdministrationModels.User;
import java.util.Optional;

public interface UserAdministrationRepository {
    Options options() throws UserAdministrationException;

    Page users(UserAdministrationQuery query, Optional<String> cursor)
            throws UserAdministrationException;

    User user(long userId) throws UserAdministrationException;

    Mutation assignStandardRole(
            long userId,
            int expectedRevision,
            AppRole role,
            String idempotencyKey) throws UserAdministrationException;

    Mutation assignOperationalRole(
            long userId,
            int expectedRevision,
            AppRole role,
            long organizationId,
            String idempotencyKey) throws UserAdministrationException;

    Mutation setBlocked(
            long userId,
            int expectedRevision,
            boolean blocked,
            String idempotencyKey) throws UserAdministrationException;
}
