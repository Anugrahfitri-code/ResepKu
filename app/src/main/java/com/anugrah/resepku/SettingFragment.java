package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingFragment extends Fragment {

    private static final String PREF_NAME = "resepku_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_THEME = "theme";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_DAILY_NOTIFICATION = "daily_notification";
    private static final String KEY_COOKING_REMINDER = "cooking_reminder";

    private static final String DEFAULT_THEME = "Light";
    private static final String DEFAULT_TEXT_SIZE = "Sedang";

    private SharedPreferences preferences;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchDailyNotification;
    private SwitchMaterial switchCookingReminder;
    private TextView tvThemeValue;
    private TextView tvTextSizeValue;
    private String selectedTheme = DEFAULT_THEME;
    private String selectedTextSize = DEFAULT_TEXT_SIZE;
    private boolean initialDarkMode;
    private boolean changingDarkMode;

    public SettingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        preferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        bindViews(view);
        loadSettings();
        setupListeners(view);

        return view;
    }

    private void bindViews(View view) {
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchDailyNotification = view.findViewById(R.id.switchDailyNotification);
        switchCookingReminder = view.findViewById(R.id.switchCookingReminder);
        tvThemeValue = view.findViewById(R.id.tvThemeValue);
        tvTextSizeValue = view.findViewById(R.id.tvTextSizeValue);
    }

    private void loadSettings() {
        boolean darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        boolean dailyNotification = preferences.getBoolean(KEY_DAILY_NOTIFICATION, true);
        boolean cookingReminder = preferences.getBoolean(KEY_COOKING_REMINDER, false);

        selectedTheme = preferences.getString(KEY_THEME, DEFAULT_THEME);
        selectedTextSize = preferences.getString(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE);

        switchDarkMode.setChecked(darkMode);
        switchDailyNotification.setChecked(dailyNotification);
        switchCookingReminder.setChecked(cookingReminder);
        tvThemeValue.setText(selectedTheme);
        tvTextSizeValue.setText(selectedTextSize);
        initialDarkMode = darkMode;
    }

    private void setupListeners(View view) {
        view.findViewById(R.id.rowDarkMode).setOnClickListener(v ->
                switchDarkMode.setChecked(!switchDarkMode.isChecked()));

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit()
                    .putBoolean(KEY_DARK_MODE, isChecked)
                    .apply();
            applyDarkModeIfChanged(isChecked);
        });

        view.findViewById(R.id.rowDailyNotification).setOnClickListener(v ->
                switchDailyNotification.setChecked(!switchDailyNotification.isChecked()));

        view.findViewById(R.id.rowCookingReminder).setOnClickListener(v ->
                switchCookingReminder.setChecked(!switchCookingReminder.isChecked()));

        view.findViewById(R.id.rowTheme).setOnClickListener(v ->
                showOptionMenu(v, new String[]{"Light", "Orange", "Green"}, value -> {
                    selectedTheme = value;
                    tvThemeValue.setText(value);
                }));

        view.findViewById(R.id.rowTextSize).setOnClickListener(v ->
                showOptionMenu(v, new String[]{"Kecil", "Sedang", "Besar"}, value -> {
                    selectedTextSize = value;
                    tvTextSizeValue.setText(value);
                }));

        view.findViewById(R.id.rowClearCache).setOnClickListener(v ->
                showToast("Cache berhasil dihapus"));

        view.findViewById(R.id.rowManageFavorite).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_favorite));

        view.findViewById(R.id.rowHelpCenter).setOnClickListener(v ->
                showToast("Pusat bantuan belum tersedia"));

        view.findViewById(R.id.rowPrivacyPolicy).setOnClickListener(v ->
                showToast("Kebijakan privasi belum tersedia"));

        view.findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());
        view.findViewById(R.id.btnResetSettings).setOnClickListener(v -> resetSettings());
    }

    private void saveSettings() {
        preferences.edit()
                .putBoolean(KEY_DARK_MODE, switchDarkMode.isChecked())
                .putString(KEY_THEME, selectedTheme)
                .putString(KEY_TEXT_SIZE, selectedTextSize)
                .putBoolean(KEY_DAILY_NOTIFICATION, switchDailyNotification.isChecked())
                .putBoolean(KEY_COOKING_REMINDER, switchCookingReminder.isChecked())
                .apply();

        showToast("Pengaturan berhasil disimpan");
        applyDarkModeIfChanged(switchDarkMode.isChecked());
    }

    private void resetSettings() {
        selectedTheme = DEFAULT_THEME;
        selectedTextSize = DEFAULT_TEXT_SIZE;

        switchDarkMode.setChecked(false);
        switchDailyNotification.setChecked(true);
        switchCookingReminder.setChecked(false);
        tvThemeValue.setText(selectedTheme);
        tvTextSizeValue.setText(selectedTextSize);

        preferences.edit()
                .putBoolean(KEY_DARK_MODE, false)
                .putString(KEY_THEME, DEFAULT_THEME)
                .putString(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
                .putBoolean(KEY_DAILY_NOTIFICATION, true)
                .putBoolean(KEY_COOKING_REMINDER, false)
                .apply();

        showToast("Pengaturan berhasil direset");
        applyDarkModeIfChanged(false);
    }

    private void applyDarkModeIfChanged(boolean enabled) {
        if (enabled == initialDarkMode || changingDarkMode) {
            return;
        }

        changingDarkMode = true;
        initialDarkMode = enabled;
        View decorView = requireActivity().getWindow().getDecorView();
        decorView.post(() -> {
                AppCompatDelegate.setDefaultNightMode(enabled
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
                decorView.postDelayed(() -> changingDarkMode = false, 500);
        });
    }

    private void showOptionMenu(View anchor, String[] options, OptionSelectedListener listener) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        for (String option : options) {
            popupMenu.getMenu().add(option);
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            listener.onSelected(item.getTitle().toString());
            return true;
        });
        popupMenu.show();
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private interface OptionSelectedListener {
        void onSelected(String value);
    }
}
