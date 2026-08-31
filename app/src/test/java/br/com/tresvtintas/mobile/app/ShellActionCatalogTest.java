package br.com.tresvtintas.mobile.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public final class ShellActionCatalogTest {
    @Test
    public void enabledAreasBecomeOrderedVisibleActions() {
        List<ShellAction> actions = ShellActionCatalog.actions(Set.of(
                MobileArea.ACCOUNT_SECURITY,
                MobileArea.CUSTOMERS,
                MobileArea.ORDERS));

        assertEquals(
                "Business actions should precede account settings.",
                List.of(
                        MobileArea.ORDERS,
                        MobileArea.CUSTOMERS,
                        MobileArea.ACCOUNT_SECURITY),
                areas(actions));
    }

    @Test
    public void agendaScopesBecomeOneNavigationAction() {
        List<ShellAction> actions = ShellActionCatalog.actions(Set.of(
                MobileArea.AGENDA,
                MobileArea.TEAM_AGENDA,
                MobileArea.GLOBAL_AGENDA));

        assertEquals(
                "The strongest agenda scope should avoid duplicate cards.",
                List.of(MobileArea.GLOBAL_AGENDA),
                areas(actions));
    }

    @Test
    public void quickActionsExcludeAgentAndSecuritySettings() {
        List<ShellAction> actions = ShellActionCatalog.quickActions(Set.of(
                MobileArea.PERSONAL_AI_AGENT,
                MobileArea.ACCOUNT_SECURITY,
                MobileArea.NOTIFICATIONS,
                MobileArea.DASHBOARD));

        assertEquals(
                "Only the operational dashboard belongs in quick actions.",
                List.of(MobileArea.DASHBOARD),
                areas(actions));
        assertFalse(
                "Quick actions must never duplicate the highlighted agent.",
                areas(actions).contains(MobileArea.PERSONAL_AI_AGENT));
    }

    @Test
    public void authorizedAuditIsAnExplicitAdministrativeAction() {
        List<ShellAction> actions = ShellActionCatalog.actions(Set.of(MobileArea.AUDIT));

        assertEquals(
                "Audit must appear once when authorization and implementation are present.",
                List.of(MobileArea.AUDIT),
                areas(actions));
    }

    @Test
    public void authorizedWhatsappAdministrationIsAnExplicitAction() {
        List<ShellAction> actions = ShellActionCatalog.actions(
                Set.of(MobileArea.WHATSAPP_INTEGRATIONS));

        assertEquals(
                "Per-store WhatsApp management must appear once when authorized.",
                List.of(MobileArea.WHATSAPP_INTEGRATIONS),
                areas(actions));
    }

    @Test
    public void navigationActionsCarryStableHumanSections() {
        List<ShellAction> actions = ShellActionCatalog.actions(Set.of(
                MobileArea.DASHBOARD,
                MobileArea.MATERIAL_QUOTES,
                MobileArea.DELIVERIES,
                MobileArea.AUDIT,
                MobileArea.ACCOUNT_SECURITY));

        assertEquals(
                List.of(
                        ShellActionSection.OVERVIEW,
                        ShellActionSection.SALES,
                        ShellActionSection.OPERATIONS,
                        ShellActionSection.MANAGEMENT,
                        ShellActionSection.PERSONAL),
                actions.stream().map(ShellAction::section).toList());
    }

    @Test
    public void navigationSectionsNeverRepeatAfterAnotherGroupStarts() {
        List<ShellAction> actions = ShellActionCatalog.actions(
                Set.copyOf(List.of(MobileArea.values())));

        int lastOrdinal = -1;
        for (ShellAction action : actions) {
            assertTrue(
                    "Menu sections must stay grouped in a stable order.",
                    action.section().ordinal() >= lastOrdinal);
            lastOrdinal = action.section().ordinal();
        }
    }

    private static List<MobileArea> areas(List<ShellAction> actions) {
        return actions.stream().map(ShellAction::area).toList();
    }
}
