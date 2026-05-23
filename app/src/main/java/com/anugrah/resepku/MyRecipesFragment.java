package com.anugrah.resepku;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MyRecipesFragment extends Fragment {
    private ActivityResultLauncher<String> imagePicker;
    private UserRecipeAdapter adapter;
    private ImageView previewImage;
    private TextView formTitle;
    private TextView categoryValue;
    private TextView levelValue;
    private TextView recipeCountText;
    private EditText titleInput;
    private EditText descriptionInput;
    private EditText timeInput;
    private EditText servingInput;
    private EditText ingredientsInput;
    private EditText stepsInput;
    private MaterialButton saveButton;
    private View emptyState;
    private NestedScrollView scrollView;
    private String editingRecipeId = "";
    private String selectedImagePath = "";

    public MyRecipesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleSelectedImage
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_recipes, container, false);
        bindViews(view);
        setupRecyclerView(view);
        setupListeners(view);
        resetForm();
        loadRecipes();
        AppThemeManager.applyToViewTree(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AppThemeManager.applyToActivity(requireActivity());
        }
        loadRecipes();
        AppThemeManager.applyToViewTree(getView());
    }

    private void bindViews(View view) {
        scrollView = view.findViewById(R.id.myRecipesScroll);
        previewImage = view.findViewById(R.id.ivUserRecipePreview);
        formTitle = view.findViewById(R.id.tvMyRecipeFormTitle);
        categoryValue = view.findViewById(R.id.tvUserRecipeCategory);
        levelValue = view.findViewById(R.id.tvUserRecipeLevel);
        recipeCountText = view.findViewById(R.id.tvUserRecipeCount);
        titleInput = view.findViewById(R.id.etUserRecipeTitle);
        descriptionInput = view.findViewById(R.id.etUserRecipeDescription);
        timeInput = view.findViewById(R.id.etUserRecipeTime);
        servingInput = view.findViewById(R.id.etUserRecipeServing);
        ingredientsInput = view.findViewById(R.id.etUserRecipeIngredients);
        stepsInput = view.findViewById(R.id.etUserRecipeSteps);
        saveButton = view.findViewById(R.id.btnSaveUserRecipe);
        emptyState = view.findViewById(R.id.emptyUserRecipes);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rvUserRecipes);
        adapter = new UserRecipeAdapter(new UserRecipeAdapter.UserRecipeActionListener() {
            @Override
            public void onRecipeClick(View itemView, UserRecipe recipe) {
                SelectedRecipeStore.setSelectedRecipe(recipe.toRecipe());
                Navigation.findNavController(itemView).navigate(R.id.navigation_recipe_detail);
            }

            @Override
            public void onEditClick(UserRecipe recipe) {
                fillFormForEdit(recipe);
            }

            @Override
            public void onDeleteClick(UserRecipe recipe) {
                confirmDelete(recipe);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void setupListeners(View view) {
        categoryValue.setOnClickListener(v -> showOptionMenu(v,
                new String[]{"Sarapan", "Ayam", "Dessert", "Seafood", "Sehat", "Daging", "Resep Saya"},
                categoryValue));
        levelValue.setOnClickListener(v -> showOptionMenu(v,
                new String[]{"Mudah", "Sedang", "Sulit"},
                levelValue));
        saveButton.setOnClickListener(v -> saveRecipe());
        view.findViewById(R.id.btnPickRecipeImage).setOnClickListener(v ->
                imagePicker.launch("image/*"));
        view.findViewById(R.id.btnClearUserRecipe).setOnClickListener(v -> resetForm());
    }

    private void handleSelectedImage(Uri uri) {
        if (uri == null || !isAdded()) {
            return;
        }

        String imagePath = UserRecipeStore.copyImageToAppStorage(requireContext(), uri);
        if (TextUtils.isEmpty(imagePath)) {
            Toast.makeText(requireContext(), "Gambar belum bisa dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedImagePath = imagePath;
        ImageLoader.load(selectedImagePath, previewImage, R.drawable.img_soup_chicken_ginger);
    }

    private void saveRecipe() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();
        String serving = servingInput.getText().toString().trim();
        List<String> ingredients = linesFrom(ingredientsInput.getText().toString());
        List<String> steps = linesFrom(stepsInput.getText().toString());

        if (title.length() < 3) {
            titleInput.setError("Nama resep minimal 3 karakter");
            titleInput.requestFocus();
            return;
        }

        if (ingredients.isEmpty()) {
            ingredientsInput.setError("Isi minimal satu bahan");
            ingredientsInput.requestFocus();
            return;
        }

        if (steps.isEmpty()) {
            stepsInput.setError("Isi minimal satu langkah");
            stepsInput.requestFocus();
            return;
        }

        UserRecipe recipe = new UserRecipe(
                TextUtils.isEmpty(editingRecipeId) ? String.valueOf(System.currentTimeMillis()) : editingRecipeId,
                title,
                categoryValue.getText().toString(),
                TextUtils.isEmpty(time) ? "30 menit" : time,
                levelValue.getText().toString(),
                TextUtils.isEmpty(serving) ? "2 porsi" : serving,
                description,
                selectedImagePath,
                ingredients,
                steps
        );
        UserRecipeStore.saveRecipe(requireContext(), recipe);
        Toast.makeText(requireContext(),
                TextUtils.isEmpty(editingRecipeId) ? "Resep buatanmu disimpan" : "Resep berhasil diperbarui",
                Toast.LENGTH_SHORT).show();
        resetForm();
        loadRecipes();
    }

    private void fillFormForEdit(UserRecipe recipe) {
        editingRecipeId = recipe.id;
        selectedImagePath = recipe.imagePath;
        formTitle.setText(R.string.my_recipe_form_edit);
        saveButton.setText(R.string.my_recipe_update);
        titleInput.setText(recipe.title);
        descriptionInput.setText(recipe.description);
        categoryValue.setText(recipe.category);
        timeInput.setText(recipe.time);
        levelValue.setText(recipe.level);
        servingInput.setText(recipe.serving);
        ingredientsInput.setText(joinLines(recipe.ingredients));
        stepsInput.setText(joinLines(recipe.steps));
        ImageLoader.load(selectedImagePath, previewImage, R.drawable.img_soup_chicken_ginger);
        AppThemeManager.applyToViewTree(requireView());
        scrollView.post(() -> scrollView.smoothScrollTo(0, requireView().findViewById(R.id.myRecipeFormCard).getTop()));
    }

    private void confirmDelete(UserRecipe recipe) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hapus resep?")
                .setMessage(recipe.title + " akan dihapus dari Resep Saya.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus", (dialog, which) -> {
                    UserRecipeStore.deleteRecipe(requireContext(), recipe.id);
                    if (recipe.id.equals(editingRecipeId)) {
                        resetForm();
                    }
                    loadRecipes();
                    Toast.makeText(requireContext(), "Resep dihapus", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void resetForm() {
        editingRecipeId = "";
        selectedImagePath = "";
        if (formTitle != null) {
            formTitle.setText(R.string.my_recipe_form_add);
        }
        if (saveButton != null) {
            saveButton.setText(R.string.my_recipe_save);
        }
        if (previewImage != null) {
            previewImage.setImageResource(R.drawable.img_soup_chicken_ginger);
        }
        titleInput.setText("");
        descriptionInput.setText("");
        categoryValue.setText(R.string.home_category_chicken);
        timeInput.setText("");
        levelValue.setText(R.string.recipe_level_easy);
        servingInput.setText("");
        ingredientsInput.setText("");
        stepsInput.setText("");
        clearErrors();
        AppThemeManager.applyToViewTree(getView());
    }

    private void loadRecipes() {
        if (!isAdded() || adapter == null) {
            return;
        }

        List<UserRecipe> recipes = UserRecipeStore.getRecipes(requireContext());
        adapter.submitList(recipes);
        emptyState.setVisibility(recipes.isEmpty() ? View.VISIBLE : View.GONE);
        recipeCountText.setText(getString(R.string.my_recipe_count, recipes.size()));
    }

    private void showOptionMenu(View anchor, String[] options, TextView target) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        for (String option : options) {
            popupMenu.getMenu().add(option);
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            target.setText(item.getTitle().toString());
            return true;
        });
        popupMenu.show();
    }

    private List<String> linesFrom(String value) {
        List<String> lines = new ArrayList<>();
        String[] parts = value.split("\\r?\\n");
        for (String part : parts) {
            String line = part.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private String joinLines(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private void clearErrors() {
        titleInput.setError(null);
        ingredientsInput.setError(null);
        stepsInput.setError(null);
    }
}
