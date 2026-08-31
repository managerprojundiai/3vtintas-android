package br.com.tresvtintas.mobile.feature.useradmin;

import android.content.Context;

@FunctionalInterface
public interface UserSecurityAccessNavigator {
    void open(Context context, long userId, String userName);
}
