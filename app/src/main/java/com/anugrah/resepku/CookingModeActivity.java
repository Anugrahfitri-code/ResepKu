package com.anugrah.resepku;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class CookingModeActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_TITLE = "com.anugrah.resepku.EXTRA_RECIPE_TITLE";

    private String[] cookingSteps;
    private int currentStep = 0;
    private TextView tvStepCounter;
    private TextView tvStepNumber;
    private TextView tvStepText;
    private MaterialButton btnPreviousStep;
    private MaterialButton btnNextStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking_mode);

        cookingSteps = new String[]{
                getString(R.string.step_1),
                getString(R.string.step_2),
                getString(R.string.step_3),
                getString(R.string.step_4),
                getString(R.string.step_5)
        };

        bindViews();
        setupHeader();
        setupActions();
        applyTheme();
        showStep(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppThemeManager.applyToActivity(this);
        AppThemeManager.applyToViewTree(findViewById(R.id.cookingModeRoot));
        applyTheme();
    }

    private void bindViews() {
        tvStepCounter = findViewById(R.id.tvStepCounter);
        tvStepNumber = findViewById(R.id.tvCookingStepNumber);
        tvStepText = findViewById(R.id.tvCookingStepText);
        btnPreviousStep = findViewById(R.id.btnPreviousStep);
        btnNextStep = findViewById(R.id.btnNextStep);
    }

    private void setupHeader() {
        String recipeTitle = getIntent().getStringExtra(EXTRA_RECIPE_TITLE);
        if (recipeTitle == null || recipeTitle.trim().isEmpty()) {
            recipeTitle = getString(R.string.detail_recipe_title);
        }
        ((TextView) findViewById(R.id.tvCookingRecipeTitle)).setText(recipeTitle);
        findViewById(R.id.btnCookingBack).setOnClickListener(v -> finish());
    }

    private void setupActions() {
        btnPreviousStep.setOnClickListener(v -> showStep(currentStep - 1));
        btnNextStep.setOnClickListener(v -> {
            if (currentStep == cookingSteps.length - 1) {
                finish();
            } else {
                showStep(currentStep + 1);
            }
        });
    }

    private void showStep(int stepIndex) {
        if (stepIndex < 0 || stepIndex >= cookingSteps.length) {
            return;
        }

        currentStep = stepIndex;
        tvStepCounter.setText(getString(R.string.cooking_step_counter, currentStep + 1, cookingSteps.length));
        tvStepNumber.setText(String.valueOf(currentStep + 1));
        tvStepText.setText(cookingSteps[currentStep]);
        btnPreviousStep.setEnabled(currentStep > 0);
        btnNextStep.setText(currentStep == cookingSteps.length - 1
                ? R.string.cooking_finish
                : R.string.cooking_next);
    }

    private void applyTheme() {
        int accent = AppThemeManager.getAccentColor(this);
        AppThemeManager.applyToActivity(this);
        AppThemeManager.applyToViewTree(findViewById(R.id.cookingModeRoot));

        btnNextStep.setBackgroundTintList(ColorStateList.valueOf(accent));
        btnNextStep.setTextColor(getColor(R.color.white));
        btnPreviousStep.setTextColor(accent);
        btnPreviousStep.setStrokeColor(ColorStateList.valueOf(accent));
        btnPreviousStep.setIconTint(ColorStateList.valueOf(accent));
        ((ImageView) findViewById(R.id.iconCookingBack)).setColorFilter(getColor(R.color.text_dark));
    }
}
