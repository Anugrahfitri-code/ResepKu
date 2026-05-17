package com.anugrah.resepku;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoriteFragment extends Fragment {

    private final List<FavoriteRecipeItem> favoriteRecipes = new ArrayList<>();
    private String selectedCategory = "";
    private String searchQuery = "";

    public FavoriteFragment() {
        // Required empty public constructor
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

        setupFavoriteRecipes(view);
        setupSearch(view);
        setupCategories(view);
        setupClicks(view);
        updateFavoriteCount();

        return view;
    }

    private void setupFavoriteRecipes(View view) {
        favoriteRecipes.clear();
        favoriteRecipes.add(new FavoriteRecipeItem(view.findViewById(R.id.cardFavoriteSoup),
                "Sup Ayam Jahe Hangat", "Ayam", true));
        favoriteRecipes.add(new FavoriteRecipeItem(view.findViewById(R.id.cardFavoriteNasi),
                "Nasi Goreng Spesial", "Sarapan", true));
        favoriteRecipes.add(new FavoriteRecipeItem(view.findViewById(R.id.cardFavoritePancake),
                "Pancake Pisang", "Dessert", true));
        favoriteRecipes.add(new FavoriteRecipeItem(view.findViewById(R.id.cardFavoriteSalad),
                "Salad Segar", "Sehat", true));
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
            selectedCategory = "";
            EditText favoriteSearch = requireView().findViewById(R.id.etFavoriteSearch);
            favoriteSearch.setText("");
            Toast.makeText(requireContext(), "Filter favorit direset", Toast.LENGTH_SHORT).show();
            applyCategoryState();
            applyFavoriteFilter();
        });
    }

    private void bindCategory(View categoryView, String category) {
        categoryView.setOnClickListener(v -> {
            selectedCategory = selectedCategory.equals(category) ? "" : category;
            applyCategoryState();
            applyFavoriteFilter();
        });
    }

    private void setupClicks(View view) {
        for (FavoriteRecipeItem item : favoriteRecipes) {
            item.card.setOnClickListener(this::openRecipeDetail);

            ImageView favoriteIcon = item.card.findViewById(R.id.ivFavorite);
            favoriteIcon.setOnClickListener(v -> {
                item.favorite = !item.favorite;
                item.card.setVisibility(item.favorite ? View.VISIBLE : View.GONE);
                Toast.makeText(
                        requireContext(),
                        item.title + (item.favorite ? " ditambahkan lagi" : " dihapus dari favorit"),
                        Toast.LENGTH_SHORT
                ).show();
                updateFavoriteCount();
                applyFavoriteFilter();
            });
        }
    }

    private void openRecipeDetail(View view) {
        Navigation.findNavController(view).navigate(R.id.navigation_recipe_detail);
    }

    private void applyFavoriteFilter() {
        String normalizedQuery = searchQuery.toLowerCase(Locale.ROOT).trim();

        for (FavoriteRecipeItem item : favoriteRecipes) {
            boolean matchesFavorite = item.favorite;
            boolean matchesCategory = selectedCategory.isEmpty()
                    || item.category.equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = normalizedQuery.isEmpty()
                    || item.title.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || item.category.toLowerCase(Locale.ROOT).contains(normalizedQuery);

            item.card.setVisibility(matchesFavorite && matchesCategory && matchesSearch
                    ? View.VISIBLE
                    : View.GONE);
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
        view.setBackgroundResource(selected ? R.drawable.bg_category_selected : R.drawable.bg_category_pill);
        view.setAlpha(selected ? 1f : 0.92f);
    }

    private void updateFavoriteCount() {
        View root = getView();
        if (root == null) {
            return;
        }

        int count = 0;
        for (FavoriteRecipeItem item : favoriteRecipes) {
            if (item.favorite) {
                count++;
            }
        }

        TextView countView = root.findViewById(R.id.tvFavoriteSavedCount);
        countView.setText(String.valueOf(count));
    }

    private static class FavoriteRecipeItem {
        final View card;
        final String title;
        final String category;
        boolean favorite;

        FavoriteRecipeItem(View card, String title, String category, boolean favorite) {
            this.card = card;
            this.title = title;
            this.category = category;
            this.favorite = favorite;
        }
    }
}
