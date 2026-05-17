package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class RecipeCacheStore {
    private static final String PREF_NAME = "resepku_recipe_cache";

    private RecipeCacheStore() {
    }

    public static void saveRecipe(Context context, Recipe recipe) {
        if (recipe == null || recipe.title == null || recipe.title.trim().isEmpty()) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("title", recipe.title);
            json.put("category", recipe.category);
            json.put("time", recipe.time);
            json.put("level", recipe.level);
            json.put("imageRes", recipe.imageRes);
            json.put("imageUrl", recipe.imageUrl);
            json.put("description", recipe.description);
            json.put("ingredients", toJsonArray(recipe.ingredients));
            json.put("steps", toJsonArray(recipe.steps));
            prefs(context).edit().putString(recipe.title, json.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public static Recipe getRecipe(Context context, String title) {
        String rawJson = prefs(context).getString(title, "");
        if (rawJson == null || rawJson.isEmpty()) {
            return null;
        }

        try {
            JSONObject json = new JSONObject(rawJson);
            return new Recipe(
                    json.optString("title", title),
                    json.optString("category", "Ayam"),
                    json.optString("time", "30 menit"),
                    json.optString("level", "Mudah"),
                    json.optInt("imageRes", R.drawable.img_soup_chicken_ginger),
                    json.optString("imageUrl", ""),
                    json.optString("description", ""),
                    toStringList(json.optJSONArray("ingredients")),
                    toStringList(json.optJSONArray("steps"))
            );
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static void removeRecipe(Context context, String title) {
        prefs(context).edit().remove(title).apply();
    }

    private static JSONArray toJsonArray(List<String> values) {
        JSONArray jsonArray = new JSONArray();
        if (values == null) {
            return jsonArray;
        }

        for (String value : values) {
            jsonArray.put(value);
        }
        return jsonArray;
    }

    private static List<String> toStringList(JSONArray jsonArray) {
        List<String> values = new ArrayList<>();
        if (jsonArray == null) {
            return values;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            String value = jsonArray.optString(i, "");
            if (!value.trim().isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
