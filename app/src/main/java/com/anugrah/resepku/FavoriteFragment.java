package com.anugrah.resepku;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FavoriteFragment extends Fragment {

    private final List<Recipe> favoriteRecipes = new ArrayList<>();
    private RecipeAdapter favoriteAdapter;
    private String selectedCategory = "";
    private String searchQuery = "";
    private final String[] quickFilterCategories = {"", "Sarapan", "Ayam", "Dessert", "Sehat"};

    public FavoriteFragment() {
    }

    public static FavoriteFragment newInstance(String param1, String param2) {
        return new FavoriteFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        setupRecyclerView(view);
        setupSearch(view);
        setupCategories(view);
        loadFavoriteRecipes();
        applyCategoryState();
        applyFavoriteFilter();
        AppThemeManager.applyToViewTree(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AppThemeManager.applyToActivity(requireActivity());
        }
        loadFavoriteRecipes();
        AppThemeManager.applyToViewTree(getView());
        applyCategoryState();
        applyFavoriteFilter();
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rvFavoriteRecipes);
        favoriteAdapter = new RecipeAdapter(new RecipeAdapter.RecipeActionListener() {
            @Override
            public void onRecipeClick(View itemView, Recipe recipe) {
                SelectedRecipeStore.setSelectedRecipe(recipe);
                Navigation.findNavController(itemView).navigate(R.id.navigation_recipe_detail);
            }

            @Override
            public void onFavoriteClick(Recipe recipe, ImageView favoriteIcon) {
                FavoriteStore.setFavorite(requireContext(), recipe.title, false);
                RecipeCacheStore.removeRecipe(requireContext(), recipe.title);
                Toast.makeText(requireContext(), recipe.title + " dihapus dari favorit", Toast.LENGTH_SHORT).show();
                loadFavoriteRecipes();
                applyFavoriteFilter();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(favoriteAdapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void setupSearch(View view) {
        EditText favoriteSearch = view.findViewById(R.id.etFavoriteSearch);
        favoriteSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFavoriteFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupCategories(View view) {
        view.findViewById(R.id.categoryAllFavorites).setOnClickListener(v -> {
            selectedCategory = "";
            applyCategoryState();
            applyFavoriteFilter();
        });
        bindCategory(view.findViewById(R.id.categoryFavoriteBreakfast), "Sarapan");
        bindCategory(view.findViewById(R.id.categoryFavoriteChicken), "Ayam");
        bindCategory(view.findViewById(R.id.categoryFavoriteDessert), "Dessert");
        bindCategory(view.findViewById(R.id.categoryFavoriteHealthy), "Sehat");

        view.findViewById(R.id.btnFavoriteFilter).setOnClickListener(v -> {
            selectedCategory = nextQuickFilter();
            EditText favoriteSearch = requireView().findViewById(R.id.etFavoriteSearch);
            favoriteSearch.setText("");
            applyCategoryState();
            applyFavoriteFilter();
            scrollToSelectedCategory();
            Toast.makeText(
                    requireContext(),
                    selectedCategory.isEmpty()
                            ? "Menampilkan semua favorit"
                            : "Filter favorit: " + selectedCategory,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private String nextQuickFilter() {
        int currentIndex = 0;
        for (int i = 0; i < quickFilterCategories.length; i++) {
            if (quickFilterCategories[i].equals(selectedCategory)) {
                currentIndex = i;
                break;
            }
        }
        return quickFilterCategories[(currentIndex + 1) % quickFilterCategories.length];
    }

    private void scrollToSelectedCategory() {
        View root = getView();
        if (root == null) {
            return;
        }

        HorizontalScrollView scrollView = root.findViewById(R.id.favoriteCategoryScroll);
        View target;
        if ("Sarapan".equals(selectedCategory)) {
            target = root.findViewById(R.id.categoryFavoriteBreakfast);
        } else if ("Ayam".equals(selectedCategory)) {
            target = root.findViewById(R.id.categoryFavoriteChicken);
        } else if ("Dessert".equals(selectedCategory)) {
            target = root.findViewById(R.id.categoryFavoriteDessert);
        } else if ("Sehat".equals(selectedCategory)) {
            target = root.findViewById(R.id.categoryFavoriteHealthy);
        } else {
            target = root.findViewById(R.id.categoryAllFavorites);
        }
        scrollView.post(() -> scrollView.smoothScrollTo(target.getLeft(), 0));
    }

    private void bindCategory(View categoryView, String category) {
        categoryView.setOnClickListener(v -> {
            selectedCategory = selectedCategory.equals(category) ? "" : category;
            applyCategoryState();
            applyFavoriteFilter();
        });
    }

    private void loadFavoriteRecipes() {
        if (!isAdded()) {
            return;
        }

        favoriteRecipes.clear();
        Set<String> favoriteTitles = FavoriteStore.getFavorites(requireContext());
        for (String title : favoriteTitles) {
            Recipe cachedRecipe = RecipeCacheStore.getRecipe(requireContext(), title);
            favoriteRecipes.add(cachedRecipe == null ? fallbackRecipe(title) : cachedRecipe);
        }
    }

    private Recipe fallbackRecipe(String title) {
        if ("Sup Ayam Jahe Hangat".equals(title)) {
            return new Recipe(title, "Ayam", "30 menit", "Mudah", R.drawable.img_soup_chicken_ginger);
        }
        if ("Nasi Goreng Spesial".equals(title)) {
            return new Recipe(title, "Sarapan", "20 menit", "Mudah", R.drawable.img_nasi_goreng);
        }
        if ("Pancake Pisang".equals(title)) {
            return new Recipe(title, "Dessert", "15 menit", "Mudah", R.drawable.img_pancake_pisang);
        }
        if ("Salad Segar".equals(title)) {
            return new Recipe(title, "Sehat", "10 menit", "Mudah", R.drawable.img_salad_segar);
        }
        return new Recipe(title, "Ayam", "30 menit", "Mudah", R.drawable.img_soup_chicken_ginger);
    }

    private void applyFavoriteFilter() {
        String normalizedQuery = searchQuery.toLowerCase(Locale.ROOT).trim();
        List<Recipe> filteredRecipes = new ArrayList<>();

        for (Recipe recipe : favoriteRecipes) {
            boolean matchesCategory = selectedCategory.isEmpty()
                    || recipe.category.equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = normalizedQuery.isEmpty()
                    || recipe.title.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || recipe.category.toLowerCase(Locale.ROOT).contains(normalizedQuery);
            if (matchesCategory && matchesSearch) {
                filteredRecipes.add(recipe);
            }
        }

        if (favoriteAdapter != null) {
            favoriteAdapter.submitList(filteredRecipes);
        }
        updateFavoriteCount();
    }

    private void applyCategoryState() {
        View root = getView();
        if (root == null) {
            return;
        }

        setCategorySelected(root.findViewById(R.id.categoryAllFavorites), selectedCategory.isEmpty());
        setCategorySelected(root.findViewById(R.id.categoryFavoriteBreakfast), selectedCategory.equals("Sarapan"));
        setCategorySelected(root.findViewById(R.id.categoryFavoriteChicken), selectedCategory.equals("Ayam"));
        setCategorySelected(root.findViewById(R.id.categoryFavoriteDessert), selectedCategory.equals("Dessert"));
        setCategorySelected(root.findViewById(R.id.categoryFavoriteHealthy), selectedCategory.equals("Sehat"));
    }

    private void setCategorySelected(View view, boolean selected) {
        AppThemeManager.applyCategoryBackground(view, selected);
    }

    private void updateFavoriteCount() {
        View root = getView();
        if (root == null) {
            return;
        }

        TextView countView = root.findViewById(R.id.tvFavoriteSavedCount);
        countView.setText(String.valueOf(favoriteRecipes.size()));
    }
}
