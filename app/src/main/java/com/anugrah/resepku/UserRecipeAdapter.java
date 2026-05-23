package com.anugrah.resepku;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UserRecipeAdapter extends RecyclerView.Adapter<UserRecipeAdapter.UserRecipeViewHolder> {
    interface UserRecipeActionListener {
        void onRecipeClick(View view, UserRecipe recipe);

        void onEditClick(UserRecipe recipe);

        void onDeleteClick(UserRecipe recipe);
    }

    private final List<UserRecipe> recipes = new ArrayList<>();
    private final UserRecipeActionListener listener;

    UserRecipeAdapter(UserRecipeActionListener listener) {
        this.listener = listener;
    }

    void submitList(List<UserRecipe> newRecipes) {
        recipes.clear();
        recipes.addAll(newRecipes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserRecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_recipe, parent, false);
        return new UserRecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserRecipeViewHolder holder, int position) {
        UserRecipe recipe = recipes.get(position);
        Context context = holder.itemView.getContext();

        ImageLoader.load(recipe.imagePath, holder.ivRecipe, R.drawable.img_soup_chicken_ginger);
        holder.tvRecipeName.setText(recipe.title);
        holder.tvCategory.setText(recipe.category);
        holder.tvRecipeTime.setText(recipe.time);
        holder.tvRecipeLevel.setText(recipe.level);

        applyCategoryStyle(holder.tvCategory, recipe.category);
        holder.btnEdit.setColorFilter(AppThemeManager.getAccentColor(context));
        holder.btnDelete.setColorFilter(AppThemeManager.getAccentStrongColor(context));
        AppThemeManager.applyToViewTree(holder.itemView);

        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(v, recipe));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(recipe));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(recipe));
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

    static class UserRecipeViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivRecipe;
        final TextView tvRecipeName;
        final TextView tvCategory;
        final TextView tvRecipeTime;
        final TextView tvRecipeLevel;
        final ImageButton btnEdit;
        final ImageButton btnDelete;

        UserRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipe = itemView.findViewById(R.id.ivUserRecipe);
            tvRecipeName = itemView.findViewById(R.id.tvUserRecipeName);
            tvCategory = itemView.findViewById(R.id.tvUserRecipeCategory);
            tvRecipeTime = itemView.findViewById(R.id.tvUserRecipeTime);
            tvRecipeLevel = itemView.findViewById(R.id.tvUserRecipeLevel);
            btnEdit = itemView.findViewById(R.id.btnEditUserRecipe);
            btnDelete = itemView.findViewById(R.id.btnDeleteUserRecipe);
        }
    }
}
