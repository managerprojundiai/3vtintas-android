package br.com.tresvtintas.mobile.app;

import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

final class ShellActionCatalog {
    private static final int QUICK_ACTION_LIMIT = 4;
    private static final List<ShellAction> ACTIONS = List.of(
            action(
                    MobileArea.DASHBOARD,
                    ShellActionSection.OVERVIEW,
                    R.string.shell_action_dashboard_title,
                    R.string.shell_action_dashboard_description,
                    R.drawable.shell_ic_dashboard),
            action(
                    MobileArea.CUSTOMER_SERVICE,
                    ShellActionSection.SALES,
                    R.string.shell_action_attendance_title,
                    R.string.shell_action_attendance_description,
                    R.drawable.shell_ic_notification),
            action(
                    MobileArea.ORDERS,
                    ShellActionSection.SALES,
                    R.string.shell_action_orders_title,
                    R.string.shell_action_orders_description,
                    R.drawable.shell_ic_order),
            action(
                    MobileArea.CATALOG,
                    ShellActionSection.SALES,
                    R.string.shell_action_catalog_title,
                    R.string.shell_action_catalog_description,
                    R.drawable.shell_ic_catalog),
            action(
                    MobileArea.CUSTOMERS,
                    ShellActionSection.SALES,
                    R.string.shell_action_customers_title,
                    R.string.shell_action_customers_description,
                    R.drawable.shell_ic_customers),
            action(
                    MobileArea.MATERIAL_QUOTES,
                    ShellActionSection.SALES,
                    R.string.shell_action_material_quotes_title,
                    R.string.shell_action_material_quotes_description,
                    R.drawable.shell_ic_quote),
            action(
                    MobileArea.LABOR_QUOTES,
                    ShellActionSection.SALES,
                    R.string.shell_action_labor_quotes_title,
                    R.string.shell_action_labor_quotes_description,
                    R.drawable.shell_ic_quote),
            action(
                    MobileArea.DELIVERIES,
                    ShellActionSection.OPERATIONS,
                    R.string.shell_action_deliveries_title,
                    R.string.shell_action_deliveries_description,
                    R.drawable.shell_ic_delivery),
            action(
                    MobileArea.WORKFORCE_LOCATION,
                    ShellActionSection.OPERATIONS,
                    R.string.shell_action_location_title,
                    R.string.shell_action_location_description,
                    R.drawable.shell_ic_delivery),
            action(
                    MobileArea.COMMISSIONS,
                    ShellActionSection.OPERATIONS,
                    R.string.shell_action_commissions_title,
                    R.string.shell_action_commissions_description,
                    R.drawable.shell_ic_sales),
            action(
                    MobileArea.COMMISSION_TEAM,
                    ShellActionSection.MANAGEMENT,
                    R.string.shell_action_commission_team_title,
                    R.string.shell_action_commission_team_description,
                    R.drawable.shell_ic_sales),
            action(
                    MobileArea.TEAM,
                    ShellActionSection.OPERATIONS,
                    R.string.shell_action_team_title,
                    R.string.shell_action_team_description,
                    R.drawable.shell_ic_customers),
            action(
                    MobileArea.PAINTERS_AND_APPROVALS,
                    ShellActionSection.MANAGEMENT,
                    R.string.shell_action_painter_admin_title,
                    R.string.shell_action_painter_admin_description,
                    R.drawable.shell_ic_customers),
            action(
                    MobileArea.TEAM_AND_USERS,
                    ShellActionSection.MANAGEMENT,
                    R.string.shell_action_user_admin_title,
                    R.string.shell_action_user_admin_description,
                    R.drawable.shell_ic_security),
            action(
                    MobileArea.ORGANIZATION_ADMINISTRATION,
                    ShellActionSection.MANAGEMENT,
                    R.string.shell_action_organization_admin_title,
                    R.string.shell_action_organization_admin_description,
                    R.drawable.shell_ic_dashboard),
            action(
                    MobileArea.AUDIT,
                    ShellActionSection.MANAGEMENT,
                    R.string.shell_action_audit_title,
                    R.string.shell_action_audit_description,
                    R.drawable.shell_ic_audit),
            action(
                    MobileArea.PERSONAL_FINANCE,
                    ShellActionSection.PERSONAL,
                    R.string.shell_action_personal_finance_title,
                    R.string.shell_action_personal_finance_description,
                    R.drawable.shell_ic_finance),
            action(
                    MobileArea.CORPORATE_FINANCE,
                    ShellActionSection.MANAGEMENT,
                    R.string.shell_action_corporate_finance_title,
                    R.string.shell_action_corporate_finance_description,
                    R.drawable.shell_ic_finance),
            action(
                    MobileArea.AGENDA,
                    ShellActionSection.OPERATIONS,
                    R.string.shell_action_agenda_title,
                    R.string.shell_action_agenda_description,
                    R.drawable.shell_ic_calendar),
            action(
                    MobileArea.TEAM_AGENDA,
                    ShellActionSection.OPERATIONS,
                    R.string.shell_action_agenda_title,
                    R.string.shell_action_agenda_description,
                    R.drawable.shell_ic_calendar),
            action(
                    MobileArea.GLOBAL_AGENDA,
                    ShellActionSection.OPERATIONS,
                    R.string.shell_action_agenda_title,
                    R.string.shell_action_agenda_description,
                    R.drawable.shell_ic_calendar),
            action(
                    MobileArea.PERSONAL_AI_AGENT,
                    ShellActionSection.PERSONAL,
                    R.string.shell_action_agent_title,
                    R.string.shell_action_agent_description,
                    R.drawable.shell_ic_agent),
            action(
                    MobileArea.ACCOUNT_SECURITY,
                    ShellActionSection.PERSONAL,
                    R.string.shell_action_account_title,
                    R.string.shell_action_account_description,
                    R.drawable.shell_ic_security),
            action(
                    MobileArea.NOTIFICATIONS,
                    ShellActionSection.PERSONAL,
                    R.string.shell_action_notifications_title,
                    R.string.shell_action_notifications_description,
                    R.drawable.shell_ic_notification),
            action(
                    MobileArea.WHATSAPP_INTEGRATIONS,
                    ShellActionSection.MANAGEMENT,
                    R.string.shell_action_whatsapp_admin_title,
                    R.string.shell_action_whatsapp_admin_description,
                    R.drawable.shell_ic_notification),
            action(
                    MobileArea.SETTINGS_AND_PROFILE,
                    ShellActionSection.MANAGEMENT,
                    R.string.shell_action_system_configuration_title,
                    R.string.shell_action_system_configuration_description,
                    R.drawable.shell_ic_security));

    private ShellActionCatalog() {
        throw new AssertionError("No instances.");
    }

    static List<ShellAction> actions(Set<MobileArea> enabledAreas) {
        List<ShellAction> result = new ArrayList<>();
        boolean agendaAdded = false;
        for (ShellActionSection section : ShellActionSection.values()) {
            for (ShellAction action : ACTIONS) {
                if (action.section() != section
                        || !enabledAreas.contains(action.area())) {
                    continue;
                }
                if (isAgenda(action.area())) {
                    if (agendaAdded || !isPreferredAgenda(action.area(), enabledAreas)) {
                        continue;
                    }
                    agendaAdded = true;
                }
                result.add(action);
            }
        }
        return Collections.unmodifiableList(result);
    }

    static List<ShellAction> quickActions(Set<MobileArea> enabledAreas) {
        List<ShellAction> result = new ArrayList<>();
        for (ShellAction action : actions(enabledAreas)) {
            if (action.area() == MobileArea.PERSONAL_AI_AGENT
                    || action.area() == MobileArea.ACCOUNT_SECURITY
                    || action.area() == MobileArea.NOTIFICATIONS) {
                continue;
            }
            result.add(action);
            if (result.size() == QUICK_ACTION_LIMIT) {
                break;
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean isAgenda(MobileArea area) {
        return area == MobileArea.AGENDA
                || area == MobileArea.TEAM_AGENDA
                || area == MobileArea.GLOBAL_AGENDA;
    }

    private static boolean isPreferredAgenda(
            MobileArea area,
            Set<MobileArea> enabledAreas) {
        if (enabledAreas.contains(MobileArea.GLOBAL_AGENDA)) {
            return area == MobileArea.GLOBAL_AGENDA;
        }
        if (enabledAreas.contains(MobileArea.TEAM_AGENDA)) {
            return area == MobileArea.TEAM_AGENDA;
        }
        return area == MobileArea.AGENDA;
    }

    private static ShellAction action(
            MobileArea area,
            ShellActionSection section,
            int titleResource,
            int descriptionResource,
            int iconResource) {
        return new ShellAction(
                area,
                section,
                titleResource,
                descriptionResource,
                iconResource);
    }
}
