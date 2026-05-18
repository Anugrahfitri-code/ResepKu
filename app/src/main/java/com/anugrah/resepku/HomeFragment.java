package com.anugrah.resepku;

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
    private final Set<String> apiRecipeTitles = new HashSet<>();
    private RecipeAdapter recipeAdapter;
    private RecipeAdapter apiRecipeAdapter;
    private int pendingApiCalls = 0;
    private boolean hasApiResult = false;
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

        view.findViewById(R.id.btnViewRecipe).setOnClickListener(v -> openCurrentRecommendationDetail(v));
        view.findViewById(R.id.btnViewAllRecipes).setOnClickListener(v -> showAllRecipes(view));
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
        AppThemeManager.applyToViewTree(getView());
        applyCategoryState();
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

    private void setupRecipes() {
        recipes.clear();
        recipes.add(new Recipe("Nasi Goreng Spesial", "Sarapan", "20 menit", "Mudah", R.drawable.img_nasi_goreng));
        recipes.add(new Recipe("Ayam Teriyaki", "Ayam", "30 menit", "Mudah", R.drawable.img_ayam_teriyaki));
        recipes.add(new Recipe("Pancake Pisang", "Dessert", "15 menit", "Mudah", R.drawable.img_pancake_pisang));
        recipes.add(new Recipe("Salad Segar", "Sehat", "10 menit", "Mudah", R.drawable.img_salad_segar));
    }

    private void loadRecipesFromApi() {
        recipeApiCalls.clear();
        apiRecipes.clear();
        apiRecipeTitles.clear();
        hasApiResult = false;

        ApiRecipeQuery[] queries = new ApiRecipeQuery[]{
                new ApiRecipeQuery("chicken", "Ayam", R.drawable.img_ayam_teriyaki),
                new ApiRecipeQuery("beef", "Daging", R.drawable.img_nasi_goreng),
                new ApiRecipeQuery("pasta", "Sarapan", R.drawable.img_nasi_goreng),
                new ApiRecipeQuery("cake", "Dessert", R.drawable.img_pancake_pisang),
                new ApiRecipeQuery("salad", "Sehat", R.drawable.img_salad_segar),
                new ApiRecipeQuery("fish", "Seafood", R.drawable.img_soup_chicken_ginger)
        };

        pendingApiCalls = queries.length;
        for (ApiRecipeQuery query : queries) {
            enqueueApiRecipeQuery(query);
        }
    }

    private void enqueueApiRecipeQuery(ApiRecipeQuery query) {
        Call<MealResponse> call = RecipeApiClient.getService().searchMeals(query.keyword);
        recipeApiCalls.add(call);
        call.enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(Call<MealResponse> call, Response<MealResponse> response) {
                if (!isAdded() || call.isCanceled()) {
                    return;
                }

                MealResponse body = response.body();
                if (response.isSuccessful() && body != null && body.meals != null) {
                    addApiRecipes(body.meals, query);
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

    private void addApiRecipes(List<Meal> meals, ApiRecipeQuery query) {
        int addedForQuery = 0;
        for (Meal meal : meals) {
            if (meal.name == null || meal.name.trim().isEmpty()) {
                continue;
            }

            String title = meal.name.trim();
            if (!apiRecipeTitles.add(title.toLowerCase(Locale.ROOT))) {
                continue;
            }

            int image = query.imageRes != 0 ? query.imageRes : localImageForIndex(apiRecipes.size());
            apiRecipes.add(new Recipe(
                    title,
                    query.category,
                    estimateTime(apiRecipes.size()),
                    "Mudah",
                    image,
                    meal.thumbnailUrl,
                    descriptionFromInstructions(meal.instructions, title),
                    ingredientsFromMeal(meal),
                    stepsFromInstructions(meal.instructions)
            ));
            addedForQuery++;

            if (addedForQuery >= 3 || apiRecipes.size() >= 14) {
                break;
            }
        }
    }

    private void finishApiCall() {
        pendingApiCalls--;
        if (pendingApiCalls > 0) {
            return;
        }

        if (!apiRecipes.isEmpty()) {
            hasApiResult = true;
            RecipeCacheStore.saveApiRecipes(requireContext(), apiRecipes);
            for (Recipe recipe : apiRecipes) {
                ImageLoader.prefetch(requireContext(), recipe.imageUrl);
            }
        } else if (!hasApiResult) {
            List<Recipe> cachedApiRecipes = RecipeCacheStore.getApiRecipes(requireContext());
            if (!cachedApiRecipes.isEmpty()) {
                apiRecipes.addAll(cachedApiRecipes);
                hasApiResult = true;
                Toast.makeText(requireContext(), "Gagal mengambil API, memakai cache resep", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Gagal mengambil API, memakai resep lokal", Toast.LENGTH_SHORT).show();
            }
        }

        applyApiRecipeFilter();
    }

    private String estimateTime(int index) {
        String[] times = {"20 menit", "30 menit", "15 menit", "25 menit"};
        return times[index % times.length];
    }

    private String descriptionFromInstructions(String instructions, String title) {
        if (instructions == null || instructions.trim().isEmpty()) {
            return title + " adalah resep pilihan dari API makanan yang bisa kamu coba di rumah.";
        }

        String firstSentence = instructions.trim().split("\\.")[0].trim();
        if (firstSentence.length() > 130) {
            firstSentence = firstSentence.substring(0, 127).trim() + "...";
        }
        return firstSentence + ".";
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
        SelectedRecipeStore.setSelectedRecipe(new Recipe(
                item.favoriteTitle,
                "Ayam",
                "30 menit",
                "Mudah",
                item.imageRes,
                "",
                item.description,
                new ArrayList<>(),
                new ArrayList<>()
        ));
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
            SelectedRecipeStore.setSelectedRecipe(new Recipe(
                    item.favoriteTitle,
                    "Ayam",
                    "30 menit",
                    "Mudah",
                    item.imageRes,
                    "",
                    item.description,
                    new ArrayList<>(),
                    new ArrayList<>()
            ));
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
        for (Recipe recipe : apiRecipes) {
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
