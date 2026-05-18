package com.anugrah.resepku;

import java.util.ArrayList;
import java.util.List;

public class Recipe {
    final String title;
    final String category;
    final String time;
    final String level;
    final String serving;
    final String rating;
    final int imageRes;
    final String imageUrl;
    final String description;
    final List<String> ingredients;
    final List<String> steps;

    Recipe(String title, String category, String time, String level, int imageRes) {
        this(title, category, time, level, imageRes, "", "", new ArrayList<>(), new ArrayList<>());
    }

    Recipe(String title, String category, String time, String level, int imageRes, String imageUrl,
           String description, List<String> ingredients, List<String> steps) {
        this(title, category, time, level, "4 porsi", imageRes, imageUrl, description, ingredients, steps);
    }

    Recipe(String title, String category, String time, String level, String serving, int imageRes,
           String imageUrl, String description, List<String> ingredients, List<String> steps) {
        this(title, category, time, level, serving, "4,8 (128)", imageRes, imageUrl, description, ingredients, steps);
    }

    Recipe(String title, String category, String time, String level, String serving, String rating, int imageRes,
           String imageUrl, String description, List<String> ingredients, List<String> steps) {
        this.title = title;
        this.category = category;
        this.time = time;
        this.level = level;
        this.serving = serving == null || serving.trim().isEmpty() ? "4 porsi" : serving;
        this.rating = rating == null || rating.trim().isEmpty() ? "4,8 (128)" : rating;
        this.imageRes = imageRes;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.description = description == null ? "" : description;
        this.ingredients = ingredients == null ? new ArrayList<>() : ingredients;
        this.steps = steps == null ? new ArrayList<>() : steps;
    }
}
