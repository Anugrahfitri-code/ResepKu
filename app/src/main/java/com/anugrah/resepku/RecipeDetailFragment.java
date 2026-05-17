package com.anugrah.resepku;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class RecipeDetailFragment extends Fragment {
    private static final String DETAIL_RECIPE_TITLE = "Sup Ayam Jahe Hangat";

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
        FavoriteStore.setFavorite(requireContext(), DETAIL_RECIPE_TITLE, true);
        Toast.makeText(requireContext(), "Sup Ayam Jahe Hangat disimpan ke favorit", Toast.LENGTH_SHORT).show();
    }
}
