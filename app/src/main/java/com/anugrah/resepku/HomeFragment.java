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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private final List<RecipeItem> recipes = new ArrayList<>();
    private final List<RecommendationItem> recommendations = new ArrayList<>();
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

        setupRecipes(view);
        setupRecommendations(view);
        setupSearch(view);
        setupCategories(view);
        setupClicks(view);

        view.findViewById(R.id.btnViewRecipe).setOnClickListener(v -> openRecipeDetail(v));
        view.findViewById(R.id.btnViewAllRecipes).setOnClickListener(v -> showAllRecipes(view));
        AppThemeManager.applyToViewTree(view);

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

    private void setupRecipes(View view) {
        recipes.clear();
        recipes.add(new RecipeItem(view.findViewById(R.id.cardNasiGoreng), "Nasi Goreng Spesial", "Sarapan"));
        recipes.add(new RecipeItem(view.findViewById(R.id.cardAyamTeriyaki), "Ayam Teriyaki", "Ayam"));
        recipes.add(new RecipeItem(view.findViewById(R.id.cardPancakePisang), "Pancake Pisang", "Dessert"));
        recipes.add(new RecipeItem(view.findViewById(R.id.cardSaladSegar), "Salad Segar", "Sehat"));
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
                        openRecipeDetail(v);
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
        });
    }

    private void setupClicks(View view) {
        for (RecipeItem recipe : recipes) {
            recipe.card.setOnClickListener(this::openRecipeDetail);

            ImageView favoriteIcon = recipe.card.findViewById(R.id.ivFavorite);
            favoriteIcon.setOnClickListener(v -> {
                boolean newFavoriteState = !FavoriteStore.isFavorite(requireContext(), recipe.title);
                FavoriteStore.setFavorite(requireContext(), recipe.title, newFavoriteState);
                updateFavoriteIcon(favoriteIcon, newFavoriteState);
                Toast.makeText(
                        requireContext(),
                        recipe.title + (newFavoriteState ? " disimpan ke favorit" : " dihapus dari favorit"),
                        Toast.LENGTH_SHORT
                ).show();
            });
        }
        refreshFavoriteIcons();
    }

    private void showAllRecipes(View root) {
        selectedCategory = "";
        searchQuery = "";
        EditText searchRecipe = root.findViewById(R.id.etSearchRecipe);
        searchRecipe.setText("");
        ((TextView) root.findViewById(R.id.tvPopularTitle)).setText("Semua Resep");
        applyCategoryState();
        applyRecipeFilter();
        View recipeList = root.findViewById(R.id.llPopularRecipes);
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
        boolean newFavoriteState = !FavoriteStore.isFavorite(requireContext(), item.favoriteTitle);
        FavoriteStore.setFavorite(requireContext(), item.favoriteTitle, newFavoriteState);

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

        for (RecipeItem recipe : recipes) {
            ImageView favoriteIcon = recipe.card.findViewById(R.id.ivFavorite);
            updateFavoriteIcon(favoriteIcon, FavoriteStore.isFavorite(requireContext(), recipe.title));
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

        for (RecipeItem recipe : recipes) {
            boolean matchesCategory = selectedCategory.isEmpty()
                    || recipe.category.equalsIgnoreCase(selectedCategory);
            boolean matchesQuery = normalizedQuery.isEmpty()
                    || recipe.title.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || recipe.category.toLowerCase(Locale.ROOT).contains(normalizedQuery);

            recipe.card.setVisibility(matchesCategory && matchesQuery ? View.VISIBLE : View.GONE);
        }
    }

    private static class RecipeItem {
        final View card;
        final String title;
        final String category;

        RecipeItem(View card, String title, String category) {
            this.card = card;
            this.title = title;
            this.category = category;
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
}
