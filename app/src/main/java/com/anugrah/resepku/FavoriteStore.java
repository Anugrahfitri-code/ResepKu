package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class FavoriteStore {
    private static final String PREF_NAME = "resepku_favorites";
    private static final String KEY_RECIPES = "recipes";

    private FavoriteStore() {
    }

    public static boolean isFavorite(Context context, String recipeTitle) {
        return getFavorites(context).contains(recipeTitle);
    }

    public static void setFavorite(Context context, String recipeTitle, boolean favorite) {
        Set<String> favorites = getFavorites(context);
        if (favorite) {
            favorites.add(recipeTitle);
        } else {
            favorites.remove(recipeTitle);
        }
        prefs(context).edit().putStringSet(KEY_RECIPES, favorites).apply();
    }

    public static int count(Context context) {
        return getFavorites(context).size();
    }

    private static Set<String> getFavorites(Context context) {
        return new HashSet<>(prefs(context).getStringSet(KEY_RECIPES, new HashSet<>()));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
