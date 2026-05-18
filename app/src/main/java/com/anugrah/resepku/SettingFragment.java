package com.anugrah.resepku;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingFragment extends Fragment {

    private static final String PREF_NAME = "resepku_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_THEME = "theme";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_DAILY_NOTIFICATION = "daily_notification";
    private static final String KEY_COOKING_REMINDER = CookingReminderScheduler.KEY_COOKING_REMINDER;
    private static final String KEY_REMINDER_HOUR = CookingReminderScheduler.KEY_REMINDER_HOUR;
    private static final String KEY_REMINDER_MINUTE = CookingReminderScheduler.KEY_REMINDER_MINUTE;
    private static final String KEY_REMINDER_RECIPE = CookingReminderScheduler.KEY_REMINDER_RECIPE;

    private static final String DEFAULT_THEME = "Orange";
    private static final String DEFAULT_TEXT_SIZE = "Sedang";

    private SharedPreferences preferences;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchDailyNotification;
    private SwitchMaterial switchCookingReminder;
    private TextView tvThemeValue;
    private TextView tvTextSizeValue;
    private TextView tvCookingReminderSummary;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private String selectedTheme = DEFAULT_THEME;
    private String selectedTextSize = DEFAULT_TEXT_SIZE;
    private String selectedReminderRecipe = "";
    private int selectedReminderHour = 8;
    private int selectedReminderMinute = 0;
    private boolean initialDarkMode;
    private boolean selectedDarkMode;
    private boolean selectedDailyNotification;
    private boolean selectedCookingReminder;
    private boolean changingDarkMode;

    public SettingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        showToast("Izin notifikasi belum diberikan");
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        preferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        bindViews(view);
        loadSettings();
        setupListeners(view);
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

    private void bindViews(View view) {
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchDailyNotification = view.findViewById(R.id.switchDailyNotification);
        switchCookingReminder = view.findViewById(R.id.switchCookingReminder);
        tvThemeValue = view.findViewById(R.id.tvThemeValue);
        tvTextSizeValue = view.findViewById(R.id.tvTextSizeValue);
        tvCookingReminderSummary = view.findViewById(R.id.tvCookingReminderSummary);
    }

    private void loadSettings() {
        boolean darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        boolean dailyNotification = preferences.getBoolean(KEY_DAILY_NOTIFICATION, true);
        boolean cookingReminder = preferences.getBoolean(KEY_COOKING_REMINDER, false);

        selectedTheme = AppThemeManager.getTheme(requireContext());
        selectedTextSize = AppThemeManager.getTextSize(requireContext());
        selectedReminderHour = preferences.getInt(KEY_REMINDER_HOUR, 8);
        selectedReminderMinute = preferences.getInt(KEY_REMINDER_MINUTE, 0);
        selectedReminderRecipe = preferences.getString(KEY_REMINDER_RECIPE, "");
        selectedDarkMode = darkMode;
        selectedDailyNotification = dailyNotification;
        selectedCookingReminder = cookingReminder;

        setSwitchesWithoutSaving(darkMode, dailyNotification, cookingReminder);
        tvThemeValue.setText(selectedTheme);
        tvTextSizeValue.setText(selectedTextSize);
        updateCookingReminderSummary();
        initialDarkMode = darkMode;
    }

    private void setupListeners(View view) {
        view.findViewById(R.id.rowDarkMode).setOnClickListener(v ->
                switchDarkMode.setChecked(!switchDarkMode.isChecked()));

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                selectedDarkMode = isChecked);

        view.findViewById(R.id.rowDailyNotification).setOnClickListener(v ->
                switchDailyNotification.setChecked(!switchDailyNotification.isChecked()));

        switchDailyNotification.setOnCheckedChangeListener((buttonView, isChecked) ->
                selectedDailyNotification = isChecked);

        view.findViewById(R.id.rowCookingReminder).setOnClickListener(v -> showCookingReminderDialog());

        switchCookingReminder.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleCookingReminderSwitch(isChecked));

        view.findViewById(R.id.btnSettingNotification).setOnClickListener(v -> {
            switchDailyNotification.setChecked(!switchDailyNotification.isChecked());
            showToast(switchDailyNotification.isChecked()
                    ? "Notifikasi resep harian siap diaktifkan"
                    : "Notifikasi resep harian siap dimatikan");
        });

        view.findViewById(R.id.rowTheme).setOnClickListener(v ->
                showOptionMenu(v, new String[]{"Orange", "Green"}, value -> {
                    selectedTheme = value;
                    tvThemeValue.setText(value);
                    showToast("Tekan Simpan Pengaturan untuk menerapkan tema");
                }));

        view.findViewById(R.id.rowTextSize).setOnClickListener(v ->
                showOptionMenu(v, new String[]{"Kecil", "Sedang", "Besar"}, value -> {
                    selectedTextSize = value;
                    tvTextSizeValue.setText(value);
                    showToast("Tekan Simpan Pengaturan untuk menerapkan ukuran teks");
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
                .putBoolean(KEY_DAILY_NOTIFICATION, selectedDailyNotification)
                .putBoolean(KEY_COOKING_REMINDER, selectedCookingReminder)
                .putInt(KEY_REMINDER_HOUR, selectedReminderHour)
                .putInt(KEY_REMINDER_MINUTE, selectedReminderMinute)
                .putString(KEY_REMINDER_RECIPE, selectedReminderRecipe)
                .apply();

        if (selectedCookingReminder && !selectedReminderRecipe.trim().isEmpty()) {
            boolean scheduled = CookingReminderScheduler.schedule(
                    requireContext(),
                    selectedReminderHour,
                    selectedReminderMinute,
                    selectedReminderRecipe
            );
            if (!scheduled) {
                selectedCookingReminder = false;
                setSwitchesWithoutSaving(selectedDarkMode, selectedDailyNotification, false);
                CookingReminderScheduler.cancel(requireContext());
                showToast("Pengingat gagal dijadwalkan di perangkat ini");
                return;
            }
        } else {
            CookingReminderScheduler.cancel(requireContext());
        }

        showToast("Pengaturan berhasil disimpan");
        applyDarkModeIfChanged(selectedDarkMode);
        AppThemeManager.applyToActivity(requireActivity());
        AppThemeManager.applyToViewTree(requireView());
    }

    private void resetSettings() {
        selectedTheme = DEFAULT_THEME;
        selectedTextSize = DEFAULT_TEXT_SIZE;
        selectedDarkMode = false;
        selectedDailyNotification = true;
        selectedCookingReminder = false;
        selectedReminderHour = 8;
        selectedReminderMinute = 0;
        selectedReminderRecipe = "";

        setSwitchesWithoutSaving(false, true, false);
        tvThemeValue.setText(selectedTheme);
        tvTextSizeValue.setText(selectedTextSize);
        updateCookingReminderSummary();
        CookingReminderScheduler.cancel(requireContext());

        preferences.edit()
                .putBoolean(KEY_DARK_MODE, false)
                .putString(KEY_THEME, DEFAULT_THEME)
                .putString(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
                .putBoolean(KEY_DAILY_NOTIFICATION, true)
                .putBoolean(KEY_COOKING_REMINDER, false)
                .putInt(KEY_REMINDER_HOUR, selectedReminderHour)
                .putInt(KEY_REMINDER_MINUTE, selectedReminderMinute)
                .putString(KEY_REMINDER_RECIPE, selectedReminderRecipe)
                .apply();

        showToast("Pengaturan berhasil direset");
        applyDarkModeIfChanged(false);
        AppThemeManager.applyToActivity(requireActivity());
        AppThemeManager.applyToViewTree(requireView());
    }

    private void setSwitchesWithoutSaving(boolean darkMode, boolean dailyNotification, boolean cookingReminder) {
        switchDarkMode.setOnCheckedChangeListener(null);
        switchDailyNotification.setOnCheckedChangeListener(null);
        switchCookingReminder.setOnCheckedChangeListener(null);

        switchDarkMode.setChecked(darkMode);
        switchDailyNotification.setChecked(dailyNotification);
        switchCookingReminder.setChecked(cookingReminder);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                selectedDarkMode = isChecked);
        switchDailyNotification.setOnCheckedChangeListener((buttonView, isChecked) ->
                selectedDailyNotification = isChecked);
        switchCookingReminder.setOnCheckedChangeListener((buttonView, isChecked) -> handleCookingReminderSwitch(isChecked));
    }

    private void handleCookingReminderSwitch(boolean isChecked) {
        selectedCookingReminder = isChecked;
        if (isChecked) {
            if (selectedReminderRecipe.trim().isEmpty()) {
                showCookingReminderDialog();
            } else {
                saveAndScheduleCookingReminder();
            }
        } else {
            CookingReminderScheduler.cancel(requireContext());
            preferences.edit().putBoolean(KEY_COOKING_REMINDER, false).apply();
            updateCookingReminderSummary();
            showToast("Pengingat memasak dimatikan");
        }
    }

    private void showCookingReminderDialog() {
        int hour = selectedReminderHour;
        int minute = selectedReminderMinute;
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) -> showRecipeNameDialog(selectedHour, selectedMinute),
                hour,
                minute,
                true
        );
        timePickerDialog.setTitle("Pilih jam pengingat");
        timePickerDialog.setOnCancelListener(dialog -> resetReminderIfIncomplete());
        timePickerDialog.show();
    }

    private void showRecipeNameDialog(int hour, int minute) {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setHint("Contoh: Ayam Teriyaki");
        input.setText(selectedReminderRecipe);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(18), dp(8), dp(18), dp(8));

        new AlertDialog.Builder(requireContext())
                .setTitle("Resep yang ingin dimasak")
                .setMessage("Masukkan nama resep untuk notifikasi pengingat.")
                .setView(input)
                .setNegativeButton("Batal", (dialog, which) -> resetReminderIfIncomplete())
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String recipeName = input.getText().toString().trim();
                    if (recipeName.isEmpty()) {
                        showToast("Nama resep tidak boleh kosong");
                        return;
                    }

                    selectedReminderHour = hour;
                    selectedReminderMinute = minute;
                    selectedReminderRecipe = recipeName;
                    selectedCookingReminder = true;
                    setSwitchesWithoutSaving(selectedDarkMode, selectedDailyNotification, true);
                    saveAndScheduleCookingReminder();
                })
                .setOnCancelListener(dialog -> resetReminderIfIncomplete())
                .show();
    }

    private void resetReminderIfIncomplete() {
        if (!selectedReminderRecipe.trim().isEmpty()) {
            return;
        }

        selectedCookingReminder = false;
        setSwitchesWithoutSaving(selectedDarkMode, selectedDailyNotification, false);
        updateCookingReminderSummary();
    }

    private void saveAndScheduleCookingReminder() {
        requestNotificationPermissionIfNeeded();
        preferences.edit()
                .putBoolean(KEY_COOKING_REMINDER, true)
                .putInt(KEY_REMINDER_HOUR, selectedReminderHour)
                .putInt(KEY_REMINDER_MINUTE, selectedReminderMinute)
                .putString(KEY_REMINDER_RECIPE, selectedReminderRecipe)
                .apply();

        boolean scheduled = CookingReminderScheduler.schedule(
                requireContext(),
                selectedReminderHour,
                selectedReminderMinute,
                selectedReminderRecipe
        );
        if (!scheduled) {
            selectedCookingReminder = false;
            setSwitchesWithoutSaving(selectedDarkMode, selectedDailyNotification, false);
            preferences.edit().putBoolean(KEY_COOKING_REMINDER, false).apply();
            CookingReminderScheduler.cancel(requireContext());
            updateCookingReminderSummary();
            showToast("Pengingat gagal dijadwalkan di perangkat ini");
            return;
        }
        updateCookingReminderSummary();
        showToast("Pengingat " + selectedReminderRecipe + " disetel pukul "
                + CookingReminderScheduler.formatTime(selectedReminderHour, selectedReminderMinute));
    }

    private void updateCookingReminderSummary() {
        if (tvCookingReminderSummary == null) {
            return;
        }

        if (selectedCookingReminder && !selectedReminderRecipe.trim().isEmpty()) {
            tvCookingReminderSummary.setText(
                    CookingReminderScheduler.formatTime(selectedReminderHour, selectedReminderMinute)
                            + " - " + selectedReminderRecipe
            );
        } else {
            tvCookingReminderSummary.setText(R.string.setting_cooking_reminder_empty);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
