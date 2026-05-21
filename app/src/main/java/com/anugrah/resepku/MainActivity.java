package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String SETTINGS_PREF_NAME = "resepku_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedNightMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        AppThemeManager.applyToActivity(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);
            navGraph.setStartDestination(AuthSessionStore.isSignedIn(this)
                    ? R.id.navigation_home
                    : R.id.navigation_login);
            navController.setGraph(navGraph);

            NavigationUI.setupWithNavController(navView, navController);
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                AppThemeManager.applyToActivity(this);
                if (destination.getId() == R.id.navigation_login
                        || destination.getId() == R.id.navigation_register
                        || destination.getId() == R.id.navigation_recipe_detail) {
                    navView.setVisibility(View.GONE);
                } else {
                    navView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void applySavedNightMode() {
        SharedPreferences preferences = getSharedPreferences(SETTINGS_PREF_NAME, Context.MODE_PRIVATE);
        boolean darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(darkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
