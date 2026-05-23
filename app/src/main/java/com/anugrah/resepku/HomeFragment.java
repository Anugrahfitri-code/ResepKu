package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private final List<Recipe> recipes = new ArrayList<>();
    private final List<RecommendationItem> recommendations = new ArrayList<>();
    private final List<Call<MealResponse>> recipeApiCalls = new ArrayList<>();
    private final List<Recipe> apiRecipes = new ArrayList<>();
    private final List<Recipe> cachedApiRecipeSnapshot = new ArrayList<>();
    private final Set<String> apiRecipeTitles = new HashSet<>();
    private final Set<String> apiRecipeIds = new HashSet<>();
    private static final String SETTINGS_PREF_NAME = "resepku_settings";
    private static final String KEY_DAILY_NOTIFICATION = "daily_notification";
    private static final int MAX_API_RECIPES = 100;
    private static final int MAX_RECIPES_PER_API_CATEGORY = 8;
    private static final int API_CACHE_PROGRESS_INTERVAL = 10;
    private RecipeAdapter recipeAdapter;
    private RecipeAdapter apiRecipeAdapter;
    private int pendingApiCalls = 0;
    private int scheduledApiRecipeLookups = 0;
    private int lastCachedApiRecipeCount = 0;
    private boolean hasApiResult = false;
    private boolean hasFreshApiResult = false;
    private boolean showingCachedApiRecipes = false;
    private String selectedCategory = "";
    private String searchQuery = "";
    private int currentRecommendationIndex = 0;
    private float recommendationTouchStartX = 0f;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        setupRecipes();
        setupRecyclerView(view);
        setupRecommendations(view);
        setupSearch(view);
        setupCategories(view);
        updateGreeting(view);

        view.findViewById(R.id.btnViewRecipe).setOnClickListener(v -> openCurrentRecommendationDetail(v));
        view.findViewById(R.id.btnViewAllRecipes).setOnClickListener(v -> showAllRecipes(view));
        view.findViewById(R.id.btnRefreshApiRecipes).setOnClickListener(v -> loadRecipesFromApi());
        view.findViewById(R.id.btnHomeNotification).setOnClickListener(v -> toggleDailyNotification());
        AppThemeManager.applyToViewTree(view);
        applyRecipeFilter();
        loadRecipesFromApi();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AppThemeManager.applyToActivity(requireActivity());
        }
        updateGreeting(getView());
        AppThemeManager.applyToViewTree(getView());
        applyCategoryState();
        applyApiRecipeFilter();
        refreshFavoriteIcons();
        showRecommendation(currentRecommendationIndex);
    }

    @Override
    public void onDestroyView() {
        for (Call<MealResponse> call : recipeApiCalls) {
            call.cancel();
        }
        recipeApiCalls.clear();
        super.onDestroyView();
    }

    private void updateGreeting(View view) {
        if (view == null || !isAdded()) {
            return;
        }

        TextView welcomeText = view.findViewById(R.id.tvHomeWelcome);
        welcomeText.setText(getString(R.string.home_welcome_name,
                AuthSessionStore.getFirstName(requireContext())));
    }

    private void setupRecipes() {
        recipes.clear();
        recipes.add(localRecipe("Nasi Goreng Spesial"));
        recipes.add(localRecipe("Ayam Teriyaki"));
        recipes.add(localRecipe("Pancake Pisang"));
        recipes.add(localRecipe("Salad Segar"));
    }

    private Recipe localRecipe(String title) {
        if ("Nasi Goreng Spesial".equals(title)) {
            return new Recipe(
                    title,
                    "Sarapan",
                    "20 menit",
                    "Mudah",
                    "3 porsi",
                    "5,0 (1.240)",
                    "Gunakan nasi yang sudah dingin agar teksturnya tidak lembek saat digoreng.",
                    R.drawable.img_nasi_goreng,
                    "",
                    "Nasi goreng rumahan yang gurih dengan telur, sayuran, dan aroma bawang yang menggugah selera.",
                    listOf("3 piring nasi putih", "2 butir telur", "3 siung bawang putih cincang", "4 siung bawang merah iris", "2 sdm kecap manis", "1 sdm saus tiram", "Secukupnya garam dan lada", "Mentimun dan jeruk limau"),
                    listOf("Tumis bawang putih dan bawang merah hingga harum.", "Masukkan telur, orak-arik sampai matang.", "Tambahkan nasi putih, kecap manis, saus tiram, garam, dan lada.", "Aduk dengan api besar sampai bumbu merata dan nasi sedikit kering.", "Sajikan dengan mentimun, jeruk limau, dan telur mata sapi.")
            );
        }
        if ("Ayam Teriyaki".equals(title)) {
            return new Recipe(
                    title,
                    "Ayam",
                    "30 menit",
                    "Mudah",
                    "4 porsi",
                    "4,9 (1.086)",
                    "Marinasi ayam minimal 15 menit supaya saus teriyaki lebih meresap.",
                    R.drawable.img_ayam_teriyaki,
                    "",
                    "Ayam teriyaki manis gurih dengan saus mengilap, cocok untuk lauk makan siang keluarga.",
                    listOf("500 g paha ayam fillet", "3 sdm kecap asin", "2 sdm madu", "1 sdm gula palem", "2 siung bawang putih parut", "1 ruas jahe parut", "1 sdm minyak wijen", "Irisan daun bawang dan wijen"),
                    listOf("Campur kecap asin, madu, gula palem, bawang putih, jahe, dan minyak wijen.", "Marinasi ayam dengan saus selama 15 menit.", "Panaskan wajan, masak ayam hingga kedua sisi kecokelatan.", "Tuang sisa saus dan masak sampai mengental serta ayam matang.", "Taburi daun bawang dan wijen sebelum disajikan.")
            );
        }
        if ("Pancake Pisang".equals(title)) {
            return new Recipe(
                    title,
                    "Dessert",
                    "15 menit",
                    "Mudah",
                    "6 porsi",
                    "4,9 (934)",
                    "Masak pancake dengan api kecil agar bagian dalam matang tanpa membuat permukaannya gosong.",
                    R.drawable.img_pancake_pisang,
                    "",
                    "Pancake lembut dengan pisang manis dan sirup hangat untuk sarapan atau camilan.",
                    listOf("2 buah pisang matang", "150 g tepung terigu", "1 butir telur", "180 ml susu cair", "1 sdm gula", "1 sdt baking powder", "1 sdm margarin leleh", "Madu atau sirup secukupnya"),
                    listOf("Haluskan pisang, lalu campur dengan telur, susu, dan margarin leleh.", "Masukkan tepung, gula, dan baking powder, aduk sampai adonan rata.", "Panaskan teflon anti lengket dengan api kecil.", "Tuang adonan secukupnya dan masak sampai muncul gelembung.", "Balik pancake, masak sebentar, lalu sajikan dengan madu atau sirup.")
            );
        }
        if ("Salad Segar".equals(title)) {
            return new Recipe(
                    title,
                    "Sehat",
                    "10 menit",
                    "Mudah",
                    "2 porsi",
                    "4,9 (812)",
                    "Simpan sayuran di kulkas sebelum disajikan agar salad terasa lebih segar dan renyah.",
                    R.drawable.img_salad_segar,
                    "",
                    "Salad sayur segar dengan telur dan jagung, ringan namun tetap mengenyangkan.",
                    listOf("1 mangkuk selada", "1 buah mentimun iris", "8 tomat ceri", "1/2 buah bawang bombai iris", "1 butir telur rebus", "3 sdm jagung manis", "2 sdm minyak zaitun", "1 sdm air lemon"),
                    listOf("Cuci bersih semua sayuran, lalu tiriskan.", "Iris selada, mentimun, tomat ceri, bawang bombai, dan telur rebus.", "Campur minyak zaitun, air lemon, garam, dan lada sebagai dressing.", "Masukkan sayuran, telur, dan jagung ke mangkuk.", "Tuang dressing, aduk perlahan, lalu sajikan segera.")
            );
        }
        return new Recipe(
                "Sup Ayam Jahe Hangat",
                "Ayam",
                "30 menit",
                "Mudah",
                "4 porsi",
                "4,9 (1.158)",
                "Geprek jahe sebelum direbus agar aroma hangatnya keluar maksimal.",
                R.drawable.img_soup_chicken_ginger,
                "",
                "Hangat, gurih, dan menyehatkan badan. Cocok untuk keluarga.",
                listOf("300 g ayam suwir", "1 buah kentang potong dadu", "2 cm jahe memarkan", "1 sdt garam", "1 buah wortel iris tipis", "1/2 sdt lada bubuk", "2 batang daun bawang iris", "750 ml air kaldu ayam", "2 siung bawang putih cincang", "Secukupnya daun seledri"),
                listOf("Rebus air kaldu, masukkan jahe memarkan dan bawang putih, masak hingga harum.", "Masukkan ayam suwir, wortel, dan kentang, lalu masak hingga sayuran empuk.", "Bumbui dengan garam dan lada bubuk, aduk rata dan koreksi rasa.", "Tambahkan daun bawang dan seledri, masak sebentar hingga matang.", "Sajikan selagi hangat.")
        );
    }

    private List<String> listOf(String... values) {
        List<String> list = new ArrayList<>();
        for (String value : values) {
            list.add(value);
        }
        return list;
    }

    private void loadRecipesFromApi() {
        for (Call<MealResponse> call : recipeApiCalls) {
            call.cancel();
        }
        recipeApiCalls.clear();
        apiRecipes.clear();
        cachedApiRecipeSnapshot.clear();
        apiRecipeTitles.clear();
        apiRecipeIds.clear();
        scheduledApiRecipeLookups = 0;
        lastCachedApiRecipeCount = 0;
        hasApiResult = false;
        hasFreshApiResult = false;
        showingCachedApiRecipes = false;
        setApiErrorVisible(false);
        applyApiRecipeFilter();
        showCachedApiRecipesForWarmStart();

        ApiRecipeQuery[] queries = new ApiRecipeQuery[]{
                new ApiRecipeQuery("Beef", "Daging", R.drawable.img_nasi_goreng),
                new ApiRecipeQuery("Chicken", "Ayam", R.drawable.img_ayam_teriyaki),
                new ApiRecipeQuery("Dessert", "Dessert", R.drawable.img_pancake_pisang),
                new ApiRecipeQuery("Lamb", "Daging", R.drawable.img_nasi_goreng),
                new ApiRecipeQuery("Miscellaneous", "Sarapan", R.drawable.img_nasi_goreng),
                new ApiRecipeQuery("Pasta", "Sarapan", R.drawable.img_nasi_goreng),
                new ApiRecipeQuery("Pork", "Daging", R.drawable.img_nasi_goreng),
                new ApiRecipeQuery("Seafood", "Seafood", R.drawable.img_soup_chicken_ginger),
                new ApiRecipeQuery("Side", "Sehat", R.drawable.img_salad_segar),
                new ApiRecipeQuery("Starter", "Sehat", R.drawable.img_salad_segar),
                new ApiRecipeQuery("Vegan", "Sehat", R.drawable.img_salad_segar),
                new ApiRecipeQuery("Vegetarian", "Sehat", R.drawable.img_salad_segar),
                new ApiRecipeQuery("Breakfast", "Sarapan", R.drawable.img_nasi_goreng),
                new ApiRecipeQuery("Goat", "Daging", R.drawable.img_nasi_goreng)
        };

        pendingApiCalls = queries.length;
        for (ApiRecipeQuery query : queries) {
            enqueueApiCategoryQuery(query);
        }
    }

    private void enqueueApiCategoryQuery(ApiRecipeQuery query) {
        Call<MealResponse> call = RecipeApiClient.getService().filterMealsByCategory(query.keyword);
        recipeApiCalls.add(call);
        call.enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                MealResponse body = response.body();
                if (response.isSuccessful() && body != null && body.meals != null) {
                    enqueueApiRecipeDetails(body.meals, query);
                }
                finishApiCall();
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable throwable) {
                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                finishApiCall();
            }
        });
    }

    private void enqueueApiRecipeDetails(List<Meal> meals, ApiRecipeQuery query) {
        int addedForQuery = 0;
        for (Meal meal : meals) {
            if (scheduledApiRecipeLookups >= MAX_API_RECIPES
                    || addedForQuery >= MAX_RECIPES_PER_API_CATEGORY) {
                break;
            }
            if (meal.id == null || meal.id.trim().isEmpty()
                    || meal.name == null || meal.name.trim().isEmpty()) {
                continue;
            }

            if (!apiRecipeIds.add(meal.id.trim())) {
                continue;
            }

            scheduledApiRecipeLookups++;
            addedForQuery++;
            pendingApiCalls++;
            enqueueApiRecipeDetail(meal.id.trim(), query);
        }
    }

    private void enqueueApiRecipeDetail(String mealId, ApiRecipeQuery query) {
        Call<MealResponse> call = RecipeApiClient.getService().lookupMeal(mealId);
        recipeApiCalls.add(call);
        call.enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                MealResponse body = response.body();
                if (response.isSuccessful() && body != null && body.meals != null && !body.meals.isEmpty()) {
                    addApiRecipe(body.meals.get(0), query);
                }
                finishApiCall();
            }

            @Override
            public void onFailure(Call<MealResponse> call, Throwable throwable) {
                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                finishApiCall();
            }
        });
    }

    private void addApiRecipe(Meal meal, ApiRecipeQuery query) {
        if (meal.name == null || meal.name.trim().isEmpty() || apiRecipes.size() >= MAX_API_RECIPES) {
            return;
        }

        if (!hasFreshApiResult) {
            if (showingCachedApiRecipes) {
                apiRecipes.clear();
                apiRecipeTitles.clear();
                showingCachedApiRecipes = false;
            }
            hasFreshApiResult = true;
        }

        String title = meal.name.trim();
        if (!apiRecipeTitles.add(title.toLowerCase(Locale.ROOT))) {
            return;
        }

        String category = categoryForMeal(meal, query);
        int image = query.imageRes != 0 ? query.imageRes : localImageForIndex(apiRecipes.size());
        List<String> ingredients = ingredientsFromMeal(meal);
        List<String> steps = stepsFromInstructions(meal.instructions);
        Recipe recipe = new Recipe(
                title,
                category,
                estimateTime(category, ingredients.size(), steps.size()),
                estimateDifficulty(ingredients.size(), steps.size()),
                estimateServing(category),
                estimateRating(title, ingredients.size(), steps.size()),
                tipForRecipe(title, category, ingredients),
                image,
                meal.thumbnailUrl,
                descriptionForRecipe(title, category, ingredients),
                ingredients,
                steps
        );
        apiRecipes.add(recipe);
        prefetchApiRecipeImage(recipe);
        cacheApiRecipesIfReady(false);
    }

    private String categoryForMeal(Meal meal, ApiRecipeQuery fallbackQuery) {
        String apiCategory = meal.category == null ? "" : meal.category.trim();
        if (apiCategory.isEmpty()) {
            return fallbackQuery.category;
        }

        if ("Chicken".equalsIgnoreCase(apiCategory)) {
            return "Ayam";
        }
        if ("Dessert".equalsIgnoreCase(apiCategory)) {
            return "Dessert";
        }
        if ("Seafood".equalsIgnoreCase(apiCategory)) {
            return "Seafood";
        }
        if ("Vegetarian".equalsIgnoreCase(apiCategory)
                || "Vegan".equalsIgnoreCase(apiCategory)
                || "Side".equalsIgnoreCase(apiCategory)
                || "Starter".equalsIgnoreCase(apiCategory)) {
            return "Sehat";
        }
        if ("Breakfast".equalsIgnoreCase(apiCategory)
                || "Pasta".equalsIgnoreCase(apiCategory)
                || "Miscellaneous".equalsIgnoreCase(apiCategory)) {
            return "Sarapan";
        }
        if ("Beef".equalsIgnoreCase(apiCategory)
                || "Lamb".equalsIgnoreCase(apiCategory)
                || "Pork".equalsIgnoreCase(apiCategory)
                || "Goat".equalsIgnoreCase(apiCategory)) {
            return "Daging";
        }
        return fallbackQuery.category;
    }

    private void finishApiCall() {
        pendingApiCalls--;
        if (pendingApiCalls > 0) {
            return;
        }

        if (hasFreshApiResult && !apiRecipes.isEmpty()) {
            if (!cachedApiRecipeSnapshot.isEmpty()
                    && apiRecipes.size() < cachedApiRecipeSnapshot.size()) {
                showCachedApiRecipes(new ArrayList<>(cachedApiRecipeSnapshot), false, false);
                return;
            }
            hasApiResult = true;
            showingCachedApiRecipes = false;
            setApiErrorVisible(false);
            cacheApiRecipesIfReady(true);
        } else if (!apiRecipes.isEmpty()) {
            hasApiResult = true;
            setApiErrorVisible(false);
        } else if (!hasApiResult) {
            loadCachedApiRecipesInBackground(true, false);
            return;
        }

        applyApiRecipeFilter();
    }

    private void cacheApiRecipesIfReady(boolean force) {
        if (!isAdded() || !hasFreshApiResult || apiRecipes.isEmpty()) {
            return;
        }

        int recipeCount = apiRecipes.size();
        if (!force
                && recipeCount < MAX_API_RECIPES
                && recipeCount % API_CACHE_PROGRESS_INTERVAL != 0) {
            return;
        }

        if (!force && recipeCount == lastCachedApiRecipeCount) {
            return;
        }

        lastCachedApiRecipeCount = recipeCount;
        saveApiRecipesInBackground(new ArrayList<>(apiRecipes), force);
    }

    private void saveApiRecipesInBackground(List<Recipe> recipesToCache, boolean cacheImages) {
        if (!isAdded()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        BackgroundTaskRunner.runInBackground(() -> {
            RecipeCacheStore.saveApiRecipesIfBetter(appContext, recipesToCache);
            if (cacheImages) {
                for (Recipe recipe : recipesToCache) {
                    ImageLoader.prefetchNow(appContext, recipe.imageUrl);
                }
            }
        });
    }

    private void prefetchApiRecipeImage(Recipe recipe) {
        if (!isAdded() || recipe == null || recipe.imageUrl == null || recipe.imageUrl.trim().isEmpty()) {
            return;
        }

        ImageLoader.prefetch(requireContext().getApplicationContext(), recipe.imageUrl);
    }

    private void showCachedApiRecipesForWarmStart() {
        if (!isAdded()) {
            return;
        }

        List<Recipe> cachedApiRecipes = RecipeCacheStore.getApiRecipes(requireContext().getApplicationContext());
        showCachedApiRecipes(cachedApiRecipes, false, true);
    }

    private void loadCachedApiRecipesInBackground(boolean showToast, boolean onlyWhenEmpty) {
        if (!isAdded()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();
        BackgroundTaskRunner.runInBackground(() -> {
            List<Recipe> cachedApiRecipes = RecipeCacheStore.getApiRecipes(appContext);
            BackgroundTaskRunner.runOnMain(() -> showCachedApiRecipes(cachedApiRecipes, showToast, onlyWhenEmpty));
        });
    }

    private void showCachedApiRecipes(List<Recipe> cachedApiRecipes, boolean showToast, boolean onlyWhenEmpty) {
        if (!isAdded()) {
            return;
        }

        if (onlyWhenEmpty && (hasFreshApiResult || !apiRecipes.isEmpty())) {
            return;
        }

        if (!cachedApiRecipes.isEmpty()) {
            apiRecipes.clear();
            cachedApiRecipeSnapshot.clear();
            apiRecipeTitles.clear();
            apiRecipes.addAll(cachedApiRecipes);
            cachedApiRecipeSnapshot.addAll(cachedApiRecipes);
            for (Recipe recipe : cachedApiRecipes) {
                if (recipe.title != null && !recipe.title.trim().isEmpty()) {
                    apiRecipeTitles.add(recipe.title.toLowerCase(Locale.ROOT));
                }
            }
            hasApiResult = true;
            hasFreshApiResult = false;
            showingCachedApiRecipes = true;
            setApiErrorVisible(false);
            if (showToast) {
                Toast.makeText(requireContext(), "Gagal mengambil API, memakai cache resep", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (onlyWhenEmpty) {
                return;
            }
            setApiErrorVisible(false);
            Toast.makeText(requireContext(), "Gagal mengambil API, menampilkan resep yang tersedia", Toast.LENGTH_SHORT).show();
        }
        applyApiRecipeFilter();
    }

    private void setApiErrorVisible(boolean visible) {
        View root = getView();
        if (root == null) {
            return;
        }

        View errorState = root.findViewById(R.id.apiErrorState);
        View apiList = root.findViewById(R.id.rvApiRecipes);
        errorState.setVisibility(visible ? View.VISIBLE : View.GONE);
        apiList.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private String estimateTime(String category, int ingredientCount, int stepCount) {
        int minutes;
        if ("Dessert".equalsIgnoreCase(category)) {
            minutes = 25 + stepCount * 3;
        } else if ("Daging".equalsIgnoreCase(category)) {
            minutes = 35 + stepCount * 4;
        } else if ("Seafood".equalsIgnoreCase(category)) {
            minutes = 20 + stepCount * 3;
        } else if ("Sehat".equalsIgnoreCase(category)) {
            minutes = 10 + stepCount * 2;
        } else {
            minutes = 15 + stepCount * 3;
        }

        if (ingredientCount > 8) {
            minutes += 10;
        }
        return Math.min(minutes, 90) + " menit";
    }

    private String estimateDifficulty(int ingredientCount, int stepCount) {
        int complexity = ingredientCount + stepCount;
        if (complexity <= 9) {
            return "Mudah";
        }
        if (complexity <= 15) {
            return "Sedang";
        }
        return "Sulit";
    }

    private String estimateServing(String category) {
        if ("Dessert".equalsIgnoreCase(category)) {
            return "6 porsi";
        }
        if ("Sehat".equalsIgnoreCase(category) || "Sarapan".equalsIgnoreCase(category)) {
            return "2 porsi";
        }
        if ("Seafood".equalsIgnoreCase(category)) {
            return "3 porsi";
        }
        return "4 porsi";
    }

    private String estimateRating(String title, int ingredientCount, int stepCount) {
        int seed = Math.abs(title.hashCode());
        int ratingTenths = 43 + (seed % 7);
        if (ingredientCount >= 6 && stepCount >= 4) {
            ratingTenths += 1;
        }
        ratingTenths = Math.min(ratingTenths, 49);

        int reviewers = 45 + (seed % 184);
        return (ratingTenths / 10) + "," + (ratingTenths % 10) + " (" + reviewers + ")";
    }

    private String descriptionForRecipe(String title, String category, List<String> ingredients) {
        String ingredientText = ingredients == null || ingredients.isEmpty()
                ? "bahan pilihan"
                : ingredients.get(0).replaceAll("^[0-9/.,\\s]+", "").trim();
        if (ingredientText.isEmpty()) {
            ingredientText = "bahan pilihan";
        }

        if ("Dessert".equalsIgnoreCase(category)) {
            return title + " adalah hidangan manis yang lembut dan cocok disajikan sebagai camilan keluarga.";
        }
        if ("Daging".equalsIgnoreCase(category)) {
            return title + " adalah menu berbahan " + ingredientText + " dengan rasa gurih yang cocok untuk makan utama.";
        }
        if ("Seafood".equalsIgnoreCase(category)) {
            return title + " adalah sajian laut yang segar, ringan, dan enak dinikmati saat masih hangat.";
        }
        if ("Sehat".equalsIgnoreCase(category)) {
            return title + " adalah pilihan sehat dengan bahan segar yang cocok untuk menu harian.";
        }
        return title + " adalah resep " + category.toLowerCase(Locale.ROOT) + " praktis dengan rasa lezat untuk dicoba di rumah.";
    }

    private String tipForRecipe(String title, String category, List<String> ingredients) {
        String mainIngredient = ingredients == null || ingredients.isEmpty()
                ? "bahan utama"
                : ingredients.get(0).replaceAll("^[0-9/.,\\s]+", "").trim();
        if (mainIngredient.isEmpty()) {
            mainIngredient = "bahan utama";
        }

        if ("Dessert".equalsIgnoreCase(category)) {
            return "Ayak bahan kering dan jangan terlalu lama mengaduk adonan agar teksturnya tetap lembut.";
        }
        if ("Daging".equalsIgnoreCase(category)) {
            return "Masak " + mainIngredient + " dengan api sedang agar bumbu meresap dan teksturnya tetap juicy.";
        }
        if ("Seafood".equalsIgnoreCase(category)) {
            return "Lumuri bahan laut dengan sedikit jeruk nipis sebelum dimasak agar aromanya lebih segar.";
        }
        if ("Sehat".equalsIgnoreCase(category)) {
            return "Tambahkan saus atau dressing terakhir supaya bahan segar tidak cepat layu.";
        }
        if ("Ayam".equalsIgnoreCase(category)) {
            return "Marinasi ayam lebih dulu agar rasa bumbu masuk sampai ke bagian dalam.";
        }
        return "Siapkan semua bahan sebelum memasak agar proses membuat " + title + " lebih lancar.";
    }

    private List<String> ingredientsFromMeal(Meal meal) {
        List<String> ingredients = new ArrayList<>();
        addIngredient(ingredients, meal.measure1, meal.ingredient1);
        addIngredient(ingredients, meal.measure2, meal.ingredient2);
        addIngredient(ingredients, meal.measure3, meal.ingredient3);
        addIngredient(ingredients, meal.measure4, meal.ingredient4);
        addIngredient(ingredients, meal.measure5, meal.ingredient5);
        addIngredient(ingredients, meal.measure6, meal.ingredient6);
        addIngredient(ingredients, meal.measure7, meal.ingredient7);
        addIngredient(ingredients, meal.measure8, meal.ingredient8);
        addIngredient(ingredients, meal.measure9, meal.ingredient9);
        addIngredient(ingredients, meal.measure10, meal.ingredient10);
        addIngredient(ingredients, meal.measure11, meal.ingredient11);
        addIngredient(ingredients, meal.measure12, meal.ingredient12);
        addIngredient(ingredients, meal.measure13, meal.ingredient13);
        addIngredient(ingredients, meal.measure14, meal.ingredient14);
        addIngredient(ingredients, meal.measure15, meal.ingredient15);
        addIngredient(ingredients, meal.measure16, meal.ingredient16);
        addIngredient(ingredients, meal.measure17, meal.ingredient17);
        addIngredient(ingredients, meal.measure18, meal.ingredient18);
        addIngredient(ingredients, meal.measure19, meal.ingredient19);
        addIngredient(ingredients, meal.measure20, meal.ingredient20);
        return ingredients;
    }

    private void addIngredient(List<String> ingredients, String measure, String ingredient) {
        if (ingredient == null || ingredient.trim().isEmpty()) {
            return;
        }

        String value = ingredient.trim();
        if (measure != null && !measure.trim().isEmpty()) {
            value = measure.trim() + " " + value;
        }
        ingredients.add(value);
    }

    private List<String> stepsFromInstructions(String instructions) {
        List<String> steps = new ArrayList<>();
        if (instructions == null || instructions.trim().isEmpty()) {
            steps.add("Siapkan bahan sesuai daftar.");
            steps.add("Masak bahan utama hingga matang.");
            steps.add("Bumbui, koreksi rasa, lalu sajikan selagi hangat.");
            return steps;
        }

        String normalized = instructions
                .replace("\r", "\n")
                .replaceAll("(?i)\\bSTEP\\s*\\d+\\b[:\\-.]?", "\n")
                .replaceAll("\\n+", "\n")
                .trim();

        String[] blocks = normalized.split("\\n");
        for (String block : blocks) {
            addInstructionParts(steps, block);
        }

        if (steps.isEmpty()) {
            addInstructionParts(steps, normalized);
        }
        return steps;
    }

    private void addInstructionParts(List<String> steps, String instructionBlock) {
        if (instructionBlock == null) {
            return;
        }

        String cleanedBlock = instructionBlock
                .replaceAll("^\\s*\\d+[\\).:-]\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleanedBlock.length() < 8) {
            return;
        }

        String[] parts = cleanedBlock.split("(?<=[.!?])\\s+(?=[A-Z0-9])");
        for (String part : parts) {
            String value = part
                    .replaceAll("^\\s*\\d+[\\).:-]\\s*", "")
                    .trim();
            if (value.length() < 8 || value.matches("(?i)^step\\s*\\d+$")) {
                continue;
            }
            steps.add(value.matches(".*[.!?]$") ? value : value + ".");
        }
    }

    private int localImageForIndex(int index) {
        int[] images = {
                R.drawable.img_soup_chicken_ginger,
                R.drawable.img_ayam_teriyaki,
                R.drawable.img_nasi_goreng,
                R.drawable.img_pancake_pisang,
                R.drawable.img_salad_segar
        };
        return images[index % images.length];
    }

    private void setupRecyclerView(View view) {
        RecyclerView popularRecyclerView = view.findViewById(R.id.rvPopularRecipes);
        RecyclerView apiRecyclerView = view.findViewById(R.id.rvApiRecipes);

        recipeAdapter = createRecipeAdapter();
        apiRecipeAdapter = createRecipeAdapter();

        popularRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        popularRecyclerView.setAdapter(recipeAdapter);
        popularRecyclerView.setNestedScrollingEnabled(false);

        apiRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        apiRecyclerView.setAdapter(apiRecipeAdapter);
        apiRecyclerView.setNestedScrollingEnabled(false);
    }

    private RecipeAdapter createRecipeAdapter() {
        return new RecipeAdapter(new RecipeAdapter.RecipeActionListener() {
            @Override
            public void onRecipeClick(View itemView, Recipe recipe) {
                SelectedRecipeStore.setSelectedRecipe(recipe);
                openRecipeDetail(itemView);
            }

            @Override
            public void onFavoriteClick(Recipe recipe, ImageView favoriteIcon) {
                boolean newFavoriteState = !FavoriteStore.isFavorite(requireContext(), recipe.title);
                FavoriteStore.setFavorite(requireContext(), recipe.title, newFavoriteState);
                if (newFavoriteState) {
                    RecipeCacheStore.saveRecipe(requireContext(), recipe);
                } else {
                    RecipeCacheStore.removeRecipe(requireContext(), recipe.title);
                }
                updateFavoriteIcon(favoriteIcon, newFavoriteState);
                Toast.makeText(
                        requireContext(),
                        recipe.title + (newFavoriteState ? " disimpan ke favorit" : " dihapus dari favorit"),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void setupRecommendations(View view) {
        recommendations.clear();
        recommendations.add(new RecommendationItem(
                "Sup Ayam\nJahe Hangat",
                "Sup Ayam Jahe Hangat",
                "Hangat, gurih, dan menyehatkan badan. Cocok untuk keluarga.",
                R.drawable.img_soup_chicken_ginger
        ));
        recommendations.add(new RecommendationItem(
                "Nasi Goreng\nSpesial",
                "Nasi Goreng Spesial",
                "Gurih, praktis, dan cocok untuk sarapan keluarga.",
                R.drawable.img_nasi_goreng
        ));
        recommendations.add(new RecommendationItem(
                "Pancake\nPisang",
                "Pancake Pisang",
                "Manis lembut dengan pisang segar dan sirup hangat.",
                R.drawable.img_pancake_pisang
        ));
        recommendations.add(new RecommendationItem(
                "Salad\nSegar",
                "Salad Segar",
                "Ringan, sehat, dan penuh warna untuk menu harian.",
                R.drawable.img_salad_segar
        ));

        View recommendationCard = view.findViewById(R.id.recommendationCard);
        recommendationCard.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    recommendationTouchStartX = event.getX();
                    return true;
                case MotionEvent.ACTION_UP:
                    float deltaX = event.getX() - recommendationTouchStartX;
                    if (Math.abs(deltaX) > 60f) {
                        if (deltaX < 0) {
                            showRecommendation(currentRecommendationIndex + 1);
                        } else {
                            showRecommendation(currentRecommendationIndex - 1);
                        }
                    } else {
                        openCurrentRecommendationDetail(v);
                    }
                    return true;
                default:
                    return true;
            }
        });

        showRecommendation(0);
        ImageView recommendationFavorite = view.findViewById(R.id.ivRecommendationFavorite);
        recommendationFavorite.setOnClickListener(v -> toggleRecommendationFavorite());
    }

    private void setupSearch(View view) {
        EditText searchRecipe = view.findViewById(R.id.etSearchRecipe);
        searchRecipe.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyRecipeFilter();
                applyApiRecipeFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupCategories(View view) {
        bindCategory(view.findViewById(R.id.categoryBreakfast), "Sarapan");
        bindCategory(view.findViewById(R.id.categoryChicken), "Ayam");
        bindCategory(view.findViewById(R.id.categoryDessert), "Dessert");
        bindCategory(view.findViewById(R.id.categorySeafood), "Seafood");
        bindCategory(view.findViewById(R.id.categoryHealthy), "Sehat");
    }

    private void bindCategory(View categoryView, String category) {
        categoryView.setOnClickListener(v -> {
            selectedCategory = selectedCategory.equals(category) ? "" : category;
            applyCategoryState();
            applyRecipeFilter();
            applyApiRecipeFilter();
        });
    }

    private void showAllRecipes(View root) {
        selectedCategory = "";
        searchQuery = "";
        EditText searchRecipe = root.findViewById(R.id.etSearchRecipe);
        searchRecipe.setText("");
        ((TextView) root.findViewById(R.id.tvPopularTitle)).setText(R.string.home_popular_title);
        applyCategoryState();
        applyRecipeFilter();
        applyApiRecipeFilter();
        View recipeList = root.findViewById(R.id.rvPopularRecipes);
        NestedScrollView homeScroll = root.findViewById(R.id.homeScroll);
        recipeList.post(() -> homeScroll.smoothScrollTo(0, recipeList.getTop()));
        Toast.makeText(requireContext(), "Menampilkan semua resep", Toast.LENGTH_SHORT).show();
    }

    private void toggleDailyNotification() {
        SharedPreferences preferences = requireContext()
                .getSharedPreferences(SETTINGS_PREF_NAME, Context.MODE_PRIVATE);
        boolean enabled = !preferences.getBoolean(KEY_DAILY_NOTIFICATION, true);
        preferences.edit().putBoolean(KEY_DAILY_NOTIFICATION, enabled).apply();
        Toast.makeText(
                requireContext(),
                enabled ? "Notifikasi resep harian diaktifkan" : "Notifikasi resep harian dimatikan",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showRecommendation(int index) {
        View root = getView();
        if (root == null || recommendations.isEmpty()) {
            return;
        }

        if (index < 0) {
            index = recommendations.size() - 1;
        } else if (index >= recommendations.size()) {
            index = 0;
        }

        currentRecommendationIndex = index;
        RecommendationItem item = recommendations.get(index);

        ((TextView) root.findViewById(R.id.tvRecoTitle)).setText(item.title);
        ((TextView) root.findViewById(R.id.tvRecoDesc)).setText(item.description);
        ((ImageView) root.findViewById(R.id.ivRecoImage)).setImageResource(item.imageRes);
        updateFavoriteIcon(
                root.findViewById(R.id.ivRecommendationFavorite),
                FavoriteStore.isFavorite(requireContext(), item.favoriteTitle)
        );

        updateDot(root.findViewById(R.id.dotRecommendation1), index == 0);
        updateDot(root.findViewById(R.id.dotRecommendation2), index == 1);
        updateDot(root.findViewById(R.id.dotRecommendation3), index == 2);
        updateDot(root.findViewById(R.id.dotRecommendation4), index == 3);
    }

    private void updateDot(View dot, boolean active) {
        AppThemeManager.applyDot(dot, active);
        ViewGroup.LayoutParams params = dot.getLayoutParams();
        int size = dpToPx(active ? 9 : 8);
        params.width = size;
        params.height = size;
        dot.setLayoutParams(params);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void toggleRecommendationFavorite() {
        if (recommendations.isEmpty()) {
            return;
        }

        RecommendationItem item = recommendations.get(currentRecommendationIndex);
        SelectedRecipeStore.setSelectedRecipe(localRecipe(item.favoriteTitle));
        boolean newFavoriteState = !FavoriteStore.isFavorite(requireContext(), item.favoriteTitle);
        FavoriteStore.setFavorite(requireContext(), item.favoriteTitle, newFavoriteState);
        if (newFavoriteState) {
            RecipeCacheStore.saveRecipe(requireContext(), SelectedRecipeStore.getSelectedRecipe());
        } else {
            RecipeCacheStore.removeRecipe(requireContext(), item.favoriteTitle);
        }

        View root = getView();
        if (root != null) {
            updateFavoriteIcon(root.findViewById(R.id.ivRecommendationFavorite), newFavoriteState);
            refreshFavoriteIcons();
        }

        Toast.makeText(
                requireContext(),
                item.favoriteTitle + (newFavoriteState ? " disimpan ke favorit" : " dihapus dari favorit"),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void refreshFavoriteIcons() {
        if (!isAdded()) {
            return;
        }

        if (recipeAdapter != null) {
            recipeAdapter.refreshFavorites();
        }
        if (apiRecipeAdapter != null) {
            apiRecipeAdapter.refreshFavorites();
        }
    }

    private void updateFavoriteIcon(ImageView favoriteIcon, boolean favorite) {
        favoriteIcon.setSelected(favorite);
        favoriteIcon.setImageResource(favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        AppThemeManager.tintFavoriteIcon(favoriteIcon, favorite);
    }

    private void openRecipeDetail(View view) {
        Navigation.findNavController(view).navigate(R.id.navigation_recipe_detail);
    }

    private void openCurrentRecommendationDetail(View view) {
        if (!recommendations.isEmpty()) {
            RecommendationItem item = recommendations.get(currentRecommendationIndex);
            SelectedRecipeStore.setSelectedRecipe(localRecipe(item.favoriteTitle));
        }
        openRecipeDetail(view);
    }

    private void applyCategoryState() {
        View root = getView();
        if (root == null) {
            return;
        }

        setCategorySelected(root.findViewById(R.id.categoryBreakfast), selectedCategory.equals("Sarapan"));
        setCategorySelected(root.findViewById(R.id.categoryChicken), selectedCategory.equals("Ayam"));
        setCategorySelected(root.findViewById(R.id.categoryDessert), selectedCategory.equals("Dessert"));
        setCategorySelected(root.findViewById(R.id.categorySeafood), selectedCategory.equals("Seafood"));
        setCategorySelected(root.findViewById(R.id.categoryHealthy), selectedCategory.equals("Sehat"));
    }

    private void setCategorySelected(View view, boolean selected) {
        AppThemeManager.applyCategoryBackground(view, selected);
    }

    private void applyRecipeFilter() {
        String normalizedQuery = searchQuery.toLowerCase(Locale.ROOT).trim();

        List<Recipe> filteredRecipes = new ArrayList<>();
        for (Recipe recipe : recipes) {
            boolean matchesCategory = selectedCategory.isEmpty()
                    || recipe.category.equalsIgnoreCase(selectedCategory);
            boolean matchesQuery = normalizedQuery.isEmpty()
                    || recipe.title.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || recipe.category.toLowerCase(Locale.ROOT).contains(normalizedQuery);

            if (matchesCategory && matchesQuery) {
                filteredRecipes.add(recipe);
            }
        }

        if (recipeAdapter != null) {
            recipeAdapter.submitList(filteredRecipes);
        }
    }

    private void applyApiRecipeFilter() {
        String normalizedQuery = searchQuery.toLowerCase(Locale.ROOT).trim();

        List<Recipe> filteredRecipes = new ArrayList<>();
        for (Recipe recipe : allAvailableRecipes()) {
            boolean matchesCategory = selectedCategory.isEmpty()
                    || recipe.category.equalsIgnoreCase(selectedCategory);
            boolean matchesQuery = normalizedQuery.isEmpty()
                    || recipe.title.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || recipe.category.toLowerCase(Locale.ROOT).contains(normalizedQuery);

            if (matchesCategory && matchesQuery) {
                filteredRecipes.add(recipe);
            }
        }

        if (apiRecipeAdapter != null) {
            apiRecipeAdapter.submitList(filteredRecipes);
        }
    }

    private List<Recipe> allAvailableRecipes() {
        List<Recipe> allRecipes = new ArrayList<>();
        Set<String> titles = new HashSet<>();

        if (isAdded()) {
            for (UserRecipe userRecipe : UserRecipeStore.getRecipes(requireContext())) {
                addRecipeIfUnique(allRecipes, titles, userRecipe.toRecipe());
            }
        }

        for (Recipe recipe : recipes) {
            addRecipeIfUnique(allRecipes, titles, recipe);
        }

        for (Recipe recipe : apiRecipes) {
            addRecipeIfUnique(allRecipes, titles, recipe);
        }
        return allRecipes;
    }

    private void addRecipeIfUnique(List<Recipe> target, Set<String> titles, Recipe recipe) {
        if (recipe == null || recipe.title == null || recipe.title.trim().isEmpty()) {
            return;
        }

        String key = recipe.title.trim().toLowerCase(Locale.ROOT);
        if (titles.add(key)) {
            target.add(recipe);
        }
    }

    private static class RecommendationItem {
        final String title;
        final String favoriteTitle;
        final String description;
        final int imageRes;

        RecommendationItem(String title, String favoriteTitle, String description, int imageRes) {
            this.title = title;
            this.favoriteTitle = favoriteTitle;
            this.description = description;
            this.imageRes = imageRes;
        }
    }

    private static class ApiRecipeQuery {
        final String keyword;
        final String category;
        final int imageRes;

        ApiRecipeQuery(String keyword, String category, int imageRes) {
            this.keyword = keyword;
            this.category = category;
            this.imageRes = imageRes;
        }
    }
}
