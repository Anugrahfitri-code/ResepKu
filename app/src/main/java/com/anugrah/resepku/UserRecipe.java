package com.anugrah.resepku;

import java.util.ArrayList;
import java.util.List;

public class UserRecipe {
    final String id;
    final String title;
    final String category;
    final String time;
    final String level;
    final String serving;
    final String description;
    final String imagePath;
    final List<String> ingredients;
    final List<String> steps;

    UserRecipe(String id, String title, String category, String time, String level, String serving,
               String description, String imagePath, List<String> ingredients, List<String> steps) {
        this.id = id == null ? "" : id;
        this.title = title == null ? "" : title;
        this.category = category == null || category.trim().isEmpty() ? "Resep Saya" : category;
        this.time = time == null || time.trim().isEmpty() ? "30 menit" : time;
        this.level = level == null || level.trim().isEmpty() ? "Mudah" : level;
        this.serving = serving == null || serving.trim().isEmpty() ? "2 porsi" : serving;
        this.description = description == null ? "" : description;
        this.imagePath = imagePath == null ? "" : imagePath;
        this.ingredients = ingredients == null ? new ArrayList<>() : ingredients;
        this.steps = steps == null ? new ArrayList<>() : steps;
    }

    Recipe toRecipe() {
        return new Recipe(
                title,
                category,
                time,
                level,
                serving,
                "Buatan sendiri",
                "Sesuaikan rasa dan catatan memasak sesuai selera keluarga.",
                R.drawable.img_soup_chicken_ginger,
                imagePath,
                description,
                ingredients,
                steps
        );
    }
}
