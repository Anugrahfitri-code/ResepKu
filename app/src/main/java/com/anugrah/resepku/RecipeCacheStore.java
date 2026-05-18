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
    private static final String KEY_API_RECIPES = "__api_recipes";

    private RecipeCacheStore() {
    }

    public static void saveRecipe(Context context, Recipe recipe) {
        if (recipe == null || recipe.title == null || recipe.title.trim().isEmpty()) {
            return;
        }

        try {
            prefs(context).edit().putString(recipe.title, toJsonObject(recipe).toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public static Recipe getRecipe(Context context, String title) {
        String rawJson = prefs(context).getString(title, "");
        if (rawJson == null || rawJson.isEmpty()) {
            return null;
        }

        try {
            return fromJsonObject(new JSONObject(rawJson), title);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static void removeRecipe(Context context, String title) {
        prefs(context).edit().remove(title).apply();
    }

    public static void saveApiRecipes(Context context, List<Recipe> recipes) {
        JSONArray jsonArray = new JSONArray();
        if (recipes == null) {
            return;
        }

        try {
            for (Recipe recipe : recipes) {
                if (recipe != null && recipe.title != null && !recipe.title.trim().isEmpty()) {
                    jsonArray.put(toJsonObject(recipe));
                }
            }
            prefs(context).edit().putString(KEY_API_RECIPES, jsonArray.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public static List<Recipe> getApiRecipes(Context context) {
        List<Recipe> recipes = new ArrayList<>();
        String rawJson = prefs(context).getString(KEY_API_RECIPES, "");
        if (rawJson == null || rawJson.isEmpty()) {
            return recipes;
        }

        try {
            JSONArray jsonArray = new JSONArray(rawJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.optJSONObject(i);
                if (json != null) {
                    recipes.add(fromJsonObject(json, ""));
                }
            }
        } catch (JSONException ignored) {
        }
        return recipes;
    }

    private static JSONObject toJsonObject(Recipe recipe) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("title", recipe.title);
        json.put("category", recipe.category);
        json.put("time", recipe.time);
        json.put("level", recipe.level);
        json.put("serving", recipe.serving);
        json.put("rating", recipe.rating);
        json.put("tip", recipe.tip);
        json.put("imageRes", recipe.imageRes);
        json.put("imageUrl", recipe.imageUrl);
        json.put("description", recipe.description);
        json.put("ingredients", toJsonArray(recipe.ingredients));
        json.put("steps", toJsonArray(recipe.steps));
        return json;
    }

    private static Recipe fromJsonObject(JSONObject json, String fallbackTitle) {
        return new Recipe(
                json.optString("title", fallbackTitle),
                json.optString("category", "Ayam"),
                json.optString("time", "30 menit"),
                json.optString("level", "Mudah"),
                json.optString("serving", "4 porsi"),
                json.optString("rating", fallbackRating(json.optString("title", fallbackTitle))),
                json.optString("tip", ""),
                json.optInt("imageRes", R.drawable.img_soup_chicken_ginger),
                json.optString("imageUrl", ""),
                json.optString("description", ""),
                toStringList(json.optJSONArray("ingredients")),
                toStringList(json.optJSONArray("steps"))
        );
    }

    private static String fallbackRating(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "4,8 (128)";
        }

        int seed = Math.abs(title.hashCode());
        int ratingTenths = 43 + (seed % 7);
        int reviewers = 45 + (seed % 184);
        return (ratingTenths / 10) + "," + (ratingTenths % 10) + " (" + reviewers + ")";
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
