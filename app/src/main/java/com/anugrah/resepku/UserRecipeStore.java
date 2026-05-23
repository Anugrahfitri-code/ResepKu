package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class UserRecipeStore {
    private static final String PREF_NAME = "resepku_user_recipes";
    private static final String KEY_RECIPES = "recipes";

    private UserRecipeStore() {
    }

    public static List<UserRecipe> getRecipes(Context context) {
        List<UserRecipe> recipes = new ArrayList<>();
        String rawJson = prefs(context).getString(KEY_RECIPES, "");
        if (rawJson == null || rawJson.isEmpty()) {
            return recipes;
        }

        try {
            JSONArray jsonArray = new JSONArray(rawJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.optJSONObject(i);
                if (json != null) {
                    recipes.add(fromJsonObject(json));
                }
            }
        } catch (JSONException ignored) {
        }
        return recipes;
    }

    public static void saveRecipe(Context context, UserRecipe recipe) {
        List<UserRecipe> recipes = getRecipes(context);
        boolean replaced = false;
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).id.equals(recipe.id)) {
                recipes.set(i, recipe);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            recipes.add(0, recipe);
        }
        saveRecipes(context, recipes);
    }

    public static void deleteRecipe(Context context, String id) {
        List<UserRecipe> recipes = getRecipes(context);
        for (int i = recipes.size() - 1; i >= 0; i--) {
            if (recipes.get(i).id.equals(id)) {
                recipes.remove(i);
            }
        }
        saveRecipes(context, recipes);
    }

    public static String copyImageToAppStorage(Context context, Uri uri) {
        if (context == null || uri == null) {
            return "";
        }

        File directory = new File(context.getFilesDir(), "user_recipe_images");
        if (!directory.exists() && !directory.mkdirs()) {
            return "";
        }

        File imageFile = new File(directory, "recipe_" + System.currentTimeMillis() + ".img");
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            if (inputStream == null) {
                return "";
            }

            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return imageFile.getAbsolutePath();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void saveRecipes(Context context, List<UserRecipe> recipes) {
        JSONArray jsonArray = new JSONArray();
        try {
            for (UserRecipe recipe : recipes) {
                jsonArray.put(toJsonObject(recipe));
            }
            prefs(context).edit().putString(KEY_RECIPES, jsonArray.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    private static JSONObject toJsonObject(UserRecipe recipe) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", recipe.id);
        json.put("title", recipe.title);
        json.put("category", recipe.category);
        json.put("time", recipe.time);
        json.put("level", recipe.level);
        json.put("serving", recipe.serving);
        json.put("description", recipe.description);
        json.put("imagePath", recipe.imagePath);
        json.put("ingredients", toJsonArray(recipe.ingredients));
        json.put("steps", toJsonArray(recipe.steps));
        return json;
    }

    private static UserRecipe fromJsonObject(JSONObject json) {
        return new UserRecipe(
                json.optString("id", String.valueOf(System.currentTimeMillis())),
                json.optString("title", ""),
                json.optString("category", "Resep Saya"),
                json.optString("time", "30 menit"),
                json.optString("level", "Mudah"),
                json.optString("serving", "2 porsi"),
                json.optString("description", ""),
                json.optString("imagePath", ""),
                toStringList(json.optJSONArray("ingredients")),
                toStringList(json.optJSONArray("steps"))
        );
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
            String value = jsonArray.optString(i, "").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
