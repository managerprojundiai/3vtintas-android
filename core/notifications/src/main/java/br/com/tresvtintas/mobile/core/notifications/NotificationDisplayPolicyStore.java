package br.com.tresvtintas.mobile.core.notifications;

import java.util.Optional;

public interface NotificationDisplayPolicyStore {
    Optional<NotificationDisplayPolicy> load();

    void save(NotificationDisplayPolicy policy);

    void clear();
}
