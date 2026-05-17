package com.anugrah.resepku;

public class Recipe {
    final String title;
    final String category;
    final String time;
    final String level;
    final int imageRes;

    Recipe(String title, String category, String time, String level, int imageRes) {
        this.title = title;
        this.category = category;
        this.time = time;
        this.level = level;
        this.imageRes = imageRes;
    }
}
