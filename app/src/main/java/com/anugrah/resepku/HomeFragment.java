package com.anugrah.resepku;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private final List<RecipeItem> recipes = new ArrayList<>();
    private String selectedCategory = "";
    private String searchQuery = "";

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
        setupSearch(view);
        setupCategories(view);
        setupClicks(view);

        view.findViewById(R.id.btnViewRecipe).setOnClickListener(v -> openRecipeDetail(v));

        return view;
    }

    private void setupRecipes(View view) {
        recipes.clear();
        recipes.add(new RecipeItem(view.findViewById(R.id.cardNasiGoreng), "Nasi Goreng Spesial", "Sarapan"));
        recipes.add(new RecipeItem(view.findViewById(R.id.cardAyamTeriyaki), "Ayam Teriyaki", "Ayam"));
        recipes.add(new RecipeItem(view.findViewById(R.id.cardPancakePisang), "Pancake Pisang", "Dessert"));
        recipes.add(new RecipeItem(view.findViewById(R.id.cardSaladSegar), "Salad Segar", "Sehat"));
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
                v.setSelected(!v.isSelected());
                v.setAlpha(v.isSelected() ? 1f : 0.55f);
                Toast.makeText(requireContext(), recipe.title + " disimpan ke favorit", Toast.LENGTH_SHORT).show();
            });
        }
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
        view.setBackgroundResource(selected ? R.drawable.bg_category_selected : R.drawable.bg_category_pill);
        view.setAlpha(selected ? 1f : 0.92f);
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
}
