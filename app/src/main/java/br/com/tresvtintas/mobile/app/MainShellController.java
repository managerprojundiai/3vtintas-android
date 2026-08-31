package br.com.tresvtintas.mobile.app;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tresvtintas.mobile.app.databinding.ActivityMainBinding;
import br.com.tresvtintas.mobile.feature.shell.MobileArea;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.Set;

final class MainShellController {
    interface Listener {
        void open(MobileArea area);

        void chooseOrganization();

        void refreshAccess();

        void logout();
    }

    private static final int NAVIGATION_ID_BASE = 20_000;
    private final ActivityMainBinding binding;
    private final Listener listener;
    private final ShellActionAdapter quickActionAdapter;
    private final ShellMoreSheet moreSheet;
    private List<ShellAction> allActions = List.of();

    MainShellController(
            ActivityMainBinding binding,
            Listener listener) {
        if (binding == null || listener == null) {
            throw new IllegalArgumentException(
                    "Shell binding and listener are required.");
        }
        this.binding = binding;
        this.listener = listener;
        quickActionAdapter = new ShellActionAdapter(
                action -> listener.open(action.area()));
        moreSheet = new ShellMoreSheet(
                binding.getRoot().getContext(),
                action -> listener.open(action.area()));
        binding.shellQuickActions.setLayoutManager(
                new LinearLayoutManager(binding.getRoot().getContext()));
        binding.shellQuickActions.setAdapter(quickActionAdapter);
        binding.shellAgentCard.setOnClickListener(
                ignored -> listener.open(MobileArea.PERSONAL_AI_AGENT));
        binding.refreshAccessAction.setOnClickListener(
                ignored -> listener.refreshAccess());
        binding.changeStoreAction.setOnClickListener(
                ignored -> listener.chooseOrganization());
        binding.shellToolbar.setOnMenuItemClickListener(
                this::onToolbarItemSelected);
    }

    void render(
            Set<MobileArea> enabledAreas) {
        allActions = ShellActionCatalog.actions(enabledAreas);
        quickActionAdapter.submit(
                ShellActionCatalog.quickActions(enabledAreas));
        binding.shellAgentCard.setVisibility(
                enabledAreas.contains(MobileArea.PERSONAL_AI_AGENT)
                        ? View.VISIBLE
                        : View.GONE);
        updateToolbar(enabledAreas);
        updateNavigation(enabledAreas);
    }

    void clear() {
        moreSheet.dismiss();
        allActions = List.of();
        quickActionAdapter.submit(List.of());
        binding.shellBottomNavigation.removeAllViews();
    }

    private boolean onToolbarItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.shell_toolbar_notifications) {
            listener.open(MobileArea.NOTIFICATIONS);
            return true;
        }
        if (itemId == R.id.shell_toolbar_account) {
            listener.open(MobileArea.ACCOUNT_SECURITY);
            return true;
        }
        if (itemId == R.id.shell_toolbar_logout) {
            listener.logout();
            return true;
        }
        return false;
    }

    private void updateToolbar(Set<MobileArea> enabledAreas) {
        Menu menu = binding.shellToolbar.getMenu();
        menu.findItem(R.id.shell_toolbar_notifications).setVisible(
                enabledAreas.contains(MobileArea.NOTIFICATIONS));
        menu.findItem(R.id.shell_toolbar_account).setVisible(
                enabledAreas.contains(MobileArea.ACCOUNT_SECURITY));
    }

    private void updateNavigation(Set<MobileArea> enabledAreas) {
        List<PrimaryNavigationItem> navigationItems =
                PrimaryNavigationPolicy.items(enabledAreas);
        binding.shellBottomNavigation.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(
                binding.getRoot().getContext());
        for (int index = 0; index < navigationItems.size(); index++) {
            PrimaryNavigationItem navigationItem = navigationItems.get(index);
            MaterialButton navigationButton = (MaterialButton) inflater.inflate(
                    R.layout.main_shell_navigation_item,
                    binding.shellBottomNavigation,
                    false);
            navigationButton.setId(navigationId(index));
            navigationButton.setText(navigationItem.titleResource());
            navigationButton.setIconResource(navigationItem.iconResource());
            navigationButton.setContentDescription(navigationButton.getText());
            navigationButton.setSelected(index == 0);
            navigationButton.setOnClickListener(ignored ->
                    onNavigationItemSelected(navigationItem));
            binding.shellBottomNavigation.addView(navigationButton);
        }
    }

    private void onNavigationItemSelected(
            PrimaryNavigationItem navigationItem) {
        switch (navigationItem.destination()) {
            case HOME -> {
                binding.shellScroll.smoothScrollTo(0, 0);
            }
            case MORE -> {
                moreSheet.show(allActions);
            }
            case SALES, AGENDA, OPERATIONS -> {
                listener.open(navigationItem.target().orElseThrow());
            }
            default -> throw new IllegalStateException(
                    "Unsupported primary navigation destination.");
        }
    }

    private static int navigationId(int index) {
        return NAVIGATION_ID_BASE + index;
    }
}
