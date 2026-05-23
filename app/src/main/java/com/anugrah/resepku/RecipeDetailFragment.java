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
    private int userRating = 0;

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
        view.findViewById(R.id.btnShare).setOnClickListener(v -> shareCurrentRecipe());
        setupRatingListeners(view);
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
        RecipeCacheStore.saveRecipe(requireContext(), currentRecipe);
        Toast.makeText(requireContext(), title + " disimpan ke favorit", Toast.LENGTH_SHORT).show();
    }

    private void openCookingMode() {
        View root = getView();
        if (root != null && !areIngredientsComplete(root)) {
            showIngredientIncompleteMessage(root);
            return;
        }

        Intent intent = new Intent(requireContext(), CookingModeActivity.class);
        intent.putExtra(CookingModeActivity.EXTRA_RECIPE_TITLE,
                currentRecipe == null ? DETAIL_RECIPE_TITLE : currentRecipe.title);
        startActivity(intent);
    }

    private void shareCurrentRecipe() {
        if (currentRecipe == null) {
            currentRecipe = defaultRecipe();
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentRecipe.title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildShareText());
        startActivity(Intent.createChooser(shareIntent, "Bagikan resep"));
    }

    private String buildShareText() {
        List<String> ingredients = currentRecipe.ingredients.isEmpty()
                ? defaultIngredients()
                : currentRecipe.ingredients;
        List<String> steps = currentRecipe.steps.isEmpty() ? defaultSteps() : currentRecipe.steps;

        StringBuilder builder = new StringBuilder();
        builder.append("ResepKu - ").append(currentRecipe.title).append("\n\n");
        builder.append(detailDescription()).append("\n\n");
        builder.append("Waktu: ").append(currentRecipe.time).append("\n");
        builder.append("Tingkat kesulitan: ").append(currentRecipe.level).append("\n");
        builder.append("Porsi: ").append(currentRecipe.serving).append("\n");
        builder.append("Rating: ").append(currentRatingText()).append("\n\n");

        appendShareList(builder, "Bahan-bahan", ingredients);
        builder.append("\n");
        appendShareList(builder, "Cara memasak", steps);

        String tip = recipeTip();
        if (!tip.trim().isEmpty()) {
            builder.append("\nTips: ").append(tip).append("\n");
        }

        builder.append("\nDibagikan dari aplikasi ResepKu.");
        return builder.toString();
    }

    private void appendShareList(StringBuilder builder, String title, List<String> values) {
        builder.append(title).append(":\n");
        for (int i = 0; i < values.size(); i++) {
            builder.append(i + 1).append(". ").append(values.get(i)).append("\n");
        }
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
        bindStats(view);
        bindUserRating(view);
        bindTip(view);
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

    private void bindStats(View view) {
        ((TextView) view.findViewById(R.id.tvDetailRating)).setText(currentRatingText());
        ((TextView) view.findViewById(R.id.tvDetailTimeValue)).setText(currentRecipe.time);
        ((TextView) view.findViewById(R.id.tvDetailLevelValue)).setText(currentRecipe.level);
        ((TextView) view.findViewById(R.id.tvDetailServingValue)).setText(currentRecipe.serving);
    }

    private void setupRatingListeners(View view) {
        bindStarClick(view, R.id.starRate1, 1);
        bindStarClick(view, R.id.starRate2, 2);
        bindStarClick(view, R.id.starRate3, 3);
        bindStarClick(view, R.id.starRate4, 4);
        bindStarClick(view, R.id.starRate5, 5);
    }

    private void bindStarClick(View root, int starId, int rating) {
        root.findViewById(starId).setOnClickListener(v -> saveUserRating(root, rating));
    }

    private void saveUserRating(View root, int rating) {
        if (currentRecipe == null) {
            currentRecipe = defaultRecipe();
        }

        userRating = rating;
        RecipeRatingStore.saveRating(requireContext(), currentRecipe.title, rating);
        bindStats(root);
        bindUserRating(root);
        AppThemeManager.applyToViewTree(root);
        Toast.makeText(requireContext(), "Rating " + rating + "/5 disimpan", Toast.LENGTH_SHORT).show();
    }

    private void bindUserRating(View view) {
        userRating = RecipeRatingStore.getRating(requireContext(), currentRecipe.title);
        updateStar(view.findViewById(R.id.starRate1), userRating >= 1);
        updateStar(view.findViewById(R.id.starRate2), userRating >= 2);
        updateStar(view.findViewById(R.id.starRate3), userRating >= 3);
        updateStar(view.findViewById(R.id.starRate4), userRating >= 4);
        updateStar(view.findViewById(R.id.starRate5), userRating >= 5);

        TextView status = view.findViewById(R.id.tvUserRatingStatus);
        status.setText(userRating > 0
                ? getString(R.string.detail_rate_value, userRating, currentRatingText())
                : getString(R.string.detail_rate_empty));
        status.setTextColor(userRating > 0
                ? AppThemeManager.getAccentColor(requireContext())
                : requireContext().getColor(R.color.text_grey));
    }

    private void updateStar(ImageView star, boolean filled) {
        star.setImageResource(filled ? R.drawable.ic_star_orange : R.drawable.ic_star_outline);
        star.setAlpha(filled ? 1f : 0.65f);
    }

    private String currentRatingText() {
        int rating = currentRecipe == null
                ? 0
                : RecipeRatingStore.getRating(requireContext(), currentRecipe.title);
        return RecipeRatingStore.displayRating(currentRecipe, rating);
    }

    private void bindTip(View view) {
        ((TextView) view.findViewById(R.id.tvDetailTipBody)).setText(recipeTip());
    }

    private String recipeTip() {
        if (currentRecipe.tip != null && !currentRecipe.tip.trim().isEmpty()) {
            return currentRecipe.tip;
        }

        String category = currentRecipe.category == null ? "" : currentRecipe.category;
        if ("Dessert".equalsIgnoreCase(category)) {
            return "Gunakan bahan bersuhu ruang agar adonan lebih halus dan hasilnya lembut.";
        }
        if ("Daging".equalsIgnoreCase(category) || "Ayam".equalsIgnoreCase(category)) {
            return "Diamkan bumbu beberapa menit agar rasa lebih meresap ke bahan utama.";
        }
        if ("Seafood".equalsIgnoreCase(category)) {
            return "Jangan masak terlalu lama agar tekstur tetap lembut dan tidak amis.";
        }
        if ("Sehat".equalsIgnoreCase(category)) {
            return "Tambahkan dressing menjelang disajikan agar sayuran tetap segar.";
        }
        if ("Sarapan".equalsIgnoreCase(category)) {
            return "Gunakan api sedang supaya bumbu matang merata tanpa membuat bahan cepat kering.";
        }
        return getString(R.string.detail_tip_body);
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
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateIngredientWarning(view));
            ingredientGrid.addView(checkBox);
        }
        updateIngredientWarning(view);
    }

    private boolean areIngredientsComplete(View view) {
        GridLayout ingredientGrid = view.findViewById(R.id.detailIngredientGrid);
        if (ingredientGrid == null || ingredientGrid.getChildCount() == 0) {
            return true;
        }

        for (int i = 0; i < ingredientGrid.getChildCount(); i++) {
            View child = ingredientGrid.getChildAt(i);
            if (child instanceof CheckBox && !((CheckBox) child).isChecked()) {
                return false;
            }
        }
        return true;
    }

    private void showIngredientIncompleteMessage(View view) {
        TextView warning = view.findViewById(R.id.tvIngredientIncomplete);
        warning.setVisibility(View.VISIBLE);
        warning.setTextColor(AppThemeManager.getAccentColor(requireContext()));
        Toast.makeText(requireContext(), R.string.detail_ingredients_incomplete, Toast.LENGTH_SHORT).show();

        View ingredientGrid = view.findViewById(R.id.detailIngredientGrid);
        if (ingredientGrid != null) {
            ingredientGrid.requestFocus();
        }
    }

    private void updateIngredientWarning(View view) {
        TextView warning = view.findViewById(R.id.tvIngredientIncomplete);
        if (warning != null && areIngredientsComplete(view)) {
            warning.setVisibility(View.GONE);
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
