package com.anugrah.resepku;

public final class SelectedRecipeStore {
    private static Recipe selectedRecipe;

    private SelectedRecipeStore() {
    }

    public static void setSelectedRecipe(Recipe recipe) {
        selectedRecipe = recipe;
    }

    public static Recipe getSelectedRecipe() {
        return selectedRecipe;
    }
}
