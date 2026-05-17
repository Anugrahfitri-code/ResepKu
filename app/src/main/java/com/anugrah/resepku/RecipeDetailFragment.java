package com.anugrah.resepku;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.List;

public class RecipeDetailFragment extends Fragment {
    private static final String DETAIL_RECIPE_TITLE = "Sup Ayam Jahe Hangat";
    private Recipe currentRecipe;

    public RecipeDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe_detail, container, false);
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );
        view.findViewById(R.id.btnDetailFavorite).setOnClickListener(v -> saveCurrentRecipe());
        view.findViewById(R.id.btnSaveFavorite).setOnClickListener(v -> saveCurrentRecipe());
        view.findViewById(R.id.btnStartCooking).setOnClickListener(v -> openCookingMode());
        bindRecipeDetail(view);
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
    }

    private void saveCurrentRecipe() {
        String title = currentRecipe == null ? DETAIL_RECIPE_TITLE : currentRecipe.title;
        FavoriteStore.setFavorite(requireContext(), title, true);
        Toast.makeText(requireContext(), title + " disimpan ke favorit", Toast.LENGTH_SHORT).show();
    }

    private void openCookingMode() {
        Intent intent = new Intent(requireContext(), CookingModeActivity.class);
        intent.putExtra(CookingModeActivity.EXTRA_RECIPE_TITLE,
                currentRecipe == null ? DETAIL_RECIPE_TITLE : currentRecipe.title);
        startActivity(intent);
    }

    private void bindRecipeDetail(View view) {
        currentRecipe = SelectedRecipeStore.getSelectedRecipe();
        if (currentRecipe == null) {
            currentRecipe = defaultRecipe();
        }

        ((TextView) view.findViewById(R.id.tvDetailRecipeTitle)).setText(currentRecipe.title);
        ((TextView) view.findViewById(R.id.tvDetailRecipeDesc)).setText(detailDescription());
        ImageLoader.load(currentRecipe.imageUrl,
                view.findViewById(R.id.ivDetailRecipeImage),
                currentRecipe.imageRes);
        bindCategory(view);
        bindIngredients(view);
        bindSteps(view);
    }

    private String detailDescription() {
        if (currentRecipe.description != null && !currentRecipe.description.trim().isEmpty()) {
            return currentRecipe.description;
        }
        return getString(R.string.detail_recipe_desc);
    }

    private void bindCategory(View view) {
        TextView primaryCategory = view.findViewById(R.id.tvDetailPrimaryCategory);
        TextView secondaryCategory = view.findViewById(R.id.tvDetailSecondaryCategory);

        primaryCategory.setText(currentRecipe.category);
        styleCategory(primaryCategory, currentRecipe.category);
        if ("Sehat".equals(currentRecipe.category)) {
            secondaryCategory.setVisibility(View.GONE);
        } else {
            secondaryCategory.setVisibility(View.VISIBLE);
            secondaryCategory.setText(R.string.home_category_healthy);
            styleCategory(secondaryCategory, "Sehat");
        }
    }

    private void bindIngredients(View view) {
        GridLayout ingredientGrid = view.findViewById(R.id.detailIngredientGrid);
        ingredientGrid.removeAllViews();
        List<String> ingredients = currentRecipe.ingredients.isEmpty()
                ? defaultIngredients()
                : currentRecipe.ingredients;

        for (String ingredient : ingredients) {
            CheckBox checkBox = new CheckBox(requireContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            checkBox.setLayoutParams(params);
            checkBox.setMinHeight(dp(54));
            checkBox.setPadding(0, dp(6), dp(8), dp(6));
            checkBox.setText(ingredient);
            checkBox.setTextColor(requireContext().getColor(R.color.text_dark));
            checkBox.setTextSize(14);
            checkBox.setButtonTintList(ColorStateList.valueOf(AppThemeManager.getAccentColor(requireContext())));
            ingredientGrid.addView(checkBox);
        }
    }

    private void bindSteps(View view) {
        LinearLayout stepsContainer = view.findViewById(R.id.detailStepsContainer);
        stepsContainer.removeAllViews();
        List<String> steps = currentRecipe.steps.isEmpty() ? defaultSteps() : currentRecipe.steps;

        for (int i = 0; i < steps.size(); i++) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setMinimumHeight(dp(78));

            TextView number = new TextView(requireContext());
            LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(dp(30), dp(30));
            number.setLayoutParams(numberParams);
            number.setGravity(android.view.Gravity.CENTER);
            number.setText(String.valueOf(i + 1));
            number.setTextColor(AppThemeManager.getAccentColor(requireContext()));
            number.setTextSize(16);
            number.setTypeface(number.getTypeface(), android.graphics.Typeface.BOLD);
            number.setBackgroundResource(R.drawable.bg_recipe_tag_orange);

            TextView stepText = new TextView(requireContext());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.setMarginStart(dp(14));
            stepText.setLayoutParams(textParams);
            stepText.setText(steps.get(i));
            stepText.setTextColor(requireContext().getColor(R.color.text_grey));
            stepText.setTextSize(14);
            stepText.setLineSpacing(dp(3), 1f);

            row.addView(number);
            row.addView(stepText);
            stepsContainer.addView(row);
        }
    }

    private void styleCategory(TextView categoryView, String category) {
        if ("Dessert".equals(category)) {
            categoryView.setBackgroundResource(R.drawable.bg_recipe_tag_pink);
            categoryView.setTextColor(0xFFEE5173);
        } else if ("Sehat".equals(category)) {
            categoryView.setBackgroundResource(R.drawable.bg_recipe_tag_green);
            categoryView.setTextColor(requireContext().getColor(R.color.primary_green));
        } else {
            categoryView.setBackgroundResource(R.drawable.bg_recipe_tag_orange);
            categoryView.setTextColor(AppThemeManager.getAccentColor(requireContext()));
        }
    }

    private Recipe defaultRecipe() {
        return new Recipe(
                DETAIL_RECIPE_TITLE,
                "Ayam",
                "30 menit",
                "Mudah",
                R.drawable.img_soup_chicken_ginger,
                "",
                getString(R.string.detail_recipe_desc),
                defaultIngredients(),
                defaultSteps()
        );
    }

    private List<String> defaultIngredients() {
        List<String> ingredients = new ArrayList<>();
        ingredients.add(getString(R.string.ingredient_chicken));
        ingredients.add(getString(R.string.ingredient_potato));
        ingredients.add(getString(R.string.ingredient_ginger));
        ingredients.add(getString(R.string.ingredient_salt));
        ingredients.add(getString(R.string.ingredient_carrot));
        ingredients.add(getString(R.string.ingredient_pepper));
        ingredients.add(getString(R.string.ingredient_scallion));
        ingredients.add(getString(R.string.ingredient_stock));
        ingredients.add(getString(R.string.ingredient_garlic));
        ingredients.add(getString(R.string.ingredient_celery));
        return ingredients;
    }

    private List<String> defaultSteps() {
        List<String> steps = new ArrayList<>();
        steps.add(getString(R.string.step_1));
        steps.add(getString(R.string.step_2));
        steps.add(getString(R.string.step_3));
        steps.add(getString(R.string.step_4));
        steps.add(getString(R.string.step_5));
        return steps;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
