package com.anugrah.resepku;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RecipeApiService {
    @GET("search.php")
    Call<MealResponse> searchMeals(@Query("s") String query);

    @GET("filter.php")
    Call<MealResponse> filterMealsByCategory(@Query("c") String category);

    @GET("lookup.php")
    Call<MealResponse> lookupMeal(@Query("i") String mealId);
}
