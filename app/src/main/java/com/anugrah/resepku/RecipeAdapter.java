package com.anugrah.resepku;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    interface RecipeActionListener {
        void onRecipeClick(View view, Recipe recipe);

        void onFavoriteClick(Recipe recipe, ImageView favoriteIcon);
    }

    private final List<Recipe> recipes = new ArrayList<>();
    private final RecipeActionListener listener;

    RecipeAdapter(RecipeActionListener listener) {
        this.listener = listener;
    }

    void submitList(List<Recipe> newRecipes) {
        recipes.clear();
        recipes.addAll(newRecipes);
        notifyDataSetChanged();
    }

    void refreshFavorites() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_popular, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        Context context = holder.itemView.getContext();

        ImageLoader.load(recipe.imageUrl, holder.ivRecipe, recipe.imageRes);
        holder.tvRecipeName.setText(recipe.title);
        holder.tvCategory.setText(recipe.category);
        holder.tvRecipeTime.setText(recipe.time);
        holder.tvRecipeLevel.setText(recipe.level);

        applyCategoryStyle(holder.tvCategory, recipe.category);
        boolean favorite = FavoriteStore.isFavorite(context, recipe.title);
        holder.ivFavorite.setSelected(favorite);
        holder.ivFavorite.setImageResource(favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        AppThemeManager.tintFavoriteIcon(holder.ivFavorite, favorite);
        AppThemeManager.applyToViewTree(holder.itemView);

        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(v, recipe));
        holder.ivFavorite.setOnClickListener(v -> listener.onFavoriteClick(recipe, holder.ivFavorite));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    private void applyCategoryStyle(TextView categoryView, String category) {
        if ("Dessert".equals(category)) {
            categoryView.setBackgroundResource(R.drawable.bg_recipe_tag_pink);
            categoryView.setTextColor(0xFFEE5173);
        } else if ("Sehat".equals(category)) {
            categoryView.setBackgroundResource(R.drawable.bg_recipe_tag_green);
            categoryView.setTextColor(categoryView.getContext().getColor(R.color.primary_green));
        } else {
            categoryView.setBackgroundResource(R.drawable.bg_recipe_tag_orange);
            categoryView.setTextColor(categoryView.getContext().getColor(R.color.primary_orange));
        }
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivRecipe;
        final ImageView ivFavorite;
        final TextView tvRecipeName;
        final TextView tvCategory;
        final TextView tvRecipeTime;
        final TextView tvRecipeLevel;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipe = itemView.findViewById(R.id.ivRecipe);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvRecipeTime = itemView.findViewById(R.id.tvRecipeTime);
            tvRecipeLevel = itemView.findViewById(R.id.tvRecipeLevel);
        }
    }
}
