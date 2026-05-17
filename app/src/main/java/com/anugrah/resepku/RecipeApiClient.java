package com.anugrah.resepku;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RecipeApiClient {
    private static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1/";
    private static RecipeApiService service;

    private RecipeApiClient() {
    }

    public static RecipeApiService getService() {
        if (service == null) {
            service = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(RecipeApiService.class);
        }
        return service;
    }
}
