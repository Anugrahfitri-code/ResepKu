package com.anugrah.resepku;

import com.google.gson.annotations.SerializedName;

public class Meal {
    @SerializedName("strMeal")
    String name;

    @SerializedName("strCategory")
    String category;
}
