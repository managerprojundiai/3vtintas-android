package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.core.model.AppRole;

final class RolePresentation {
    private RolePresentation() {
        throw new AssertionError("No instances.");
    }

    static int label(AppRole role) {
        return switch (role) {
            case MASTER_ADMIN -> R.string.role_master_admin;
            case MANAGER -> R.string.role_manager;
            case SALESPERSON -> R.string.role_salesperson;
            case DELIVERY_DRIVER -> R.string.role_delivery_driver;
            case PAINTER -> R.string.role_painter;
            case CUSTOMER -> R.string.role_customer;
            case USER -> R.string.role_user;
        };
    }
}
