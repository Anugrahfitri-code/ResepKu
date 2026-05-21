package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class AuthSessionStore {
    private static final String PREF_NAME = "resepku_auth";
    private static final String KEY_SIGNED_IN = "signed_in";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";

    private AuthSessionStore() {
    }

    public static boolean isSignedIn(Context context) {
        return prefs(context).getBoolean(KEY_SIGNED_IN, false);
    }

    public static boolean hasRegisteredAccount(Context context) {
        SharedPreferences preferences = prefs(context);
        return !TextUtils.isEmpty(preferences.getString(KEY_EMAIL, ""))
                && !TextUtils.isEmpty(preferences.getString(KEY_PASSWORD, ""));
    }

    public static boolean canSignIn(Context context, String email, String password) {
        SharedPreferences preferences = prefs(context);
        String savedEmail = normalizeEmail(preferences.getString(KEY_EMAIL, ""));
        String savedPassword = preferences.getString(KEY_PASSWORD, "");
        return savedEmail.equals(normalizeEmail(email)) && savedPassword.equals(password);
    }

    public static void register(Context context, String name, String email, String password) {
        prefs(context).edit()
                .putBoolean(KEY_SIGNED_IN, true)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, normalizeEmail(email))
                .putString(KEY_PASSWORD, password)
                .apply();
    }

    public static void signIn(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_SIGNED_IN, true)
                .apply();
    }

    public static String getDisplayName(Context context) {
        String name = prefs(context).getString(KEY_NAME, "");
        return TextUtils.isEmpty(name) ? "Teman ResepKu" : name;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
