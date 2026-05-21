package com.anugrah.resepku;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

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
    private TextView tvAccountName;
    private TextView tvAccountEmail;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private List<CookingReminderStore.CookingReminder> reminders = new ArrayList<>();
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
        updateAccountSummary();
        AppThemeManager.applyToViewTree(getView());
    }

    private void bindViews(View view) {
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchDailyNotification = view.findViewById(R.id.switchDailyNotification);
        switchCookingReminder = view.findViewById(R.id.switchCookingReminder);
        tvThemeValue = view.findViewById(R.id.tvThemeValue);
        tvTextSizeValue = view.findViewById(R.id.tvTextSizeValue);
        tvCookingReminderSummary = view.findViewById(R.id.tvCookingReminderSummary);
        tvAccountName = view.findViewById(R.id.tvAccountName);
        tvAccountEmail = view.findViewById(R.id.tvAccountEmail);
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
        reminders = CookingReminderStore.getReminders(requireContext());
        selectedDarkMode = darkMode;
        selectedDailyNotification = dailyNotification;
        selectedCookingReminder = cookingReminder && !reminders.isEmpty();

        setSwitchesWithoutSaving(darkMode, dailyNotification, selectedCookingReminder);
        tvThemeValue.setText(selectedTheme);
        tvTextSizeValue.setText(selectedTextSize);
        updateAccountSummary();
        updateCookingReminderSummary();
        initialDarkMode = darkMode;
    }

    private void setupListeners(View view) {
        view.findViewById(R.id.rowEditProfile).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.rowChangePassword).setOnClickListener(v -> showChangePasswordDialog());
        view.findViewById(R.id.rowLogout).setOnClickListener(this::showLogoutDialog);

        view.findViewById(R.id.rowDarkMode).setOnClickListener(v ->
                switchDarkMode.setChecked(!switchDarkMode.isChecked()));

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                selectedDarkMode = isChecked);

        view.findViewById(R.id.rowDailyNotification).setOnClickListener(v ->
                switchDailyNotification.setChecked(!switchDailyNotification.isChecked()));

        switchDailyNotification.setOnCheckedChangeListener((buttonView, isChecked) ->
                selectedDailyNotification = isChecked);

        view.findViewById(R.id.rowCookingReminder).setOnClickListener(v -> showCookingReminderListDialog());

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

    private void updateAccountSummary() {
        if (tvAccountName == null || tvAccountEmail == null || !isAdded()) {
            return;
        }

        tvAccountName.setText(AuthSessionStore.getDisplayName(requireContext()));
        String email = AuthSessionStore.getEmail(requireContext());
        tvAccountEmail.setText(TextUtils.isEmpty(email)
                ? getString(R.string.setting_account_email_empty)
                : email);
    }

    private void showEditProfileDialog() {
        EditText nameInput = createDialogInput("Nama lengkap",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText emailInput = createDialogInput("Email",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        nameInput.setText(AuthSessionStore.getDisplayName(requireContext()));
        emailInput.setText(AuthSessionStore.getEmail(requireContext()));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Ubah Profil")
                .setView(dialogInputContainer(nameInput, emailInput))
                .setNegativeButton("Batal", null)
                .setPositiveButton("Simpan", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = nameInput.getText().toString().trim();
                    String email = emailInput.getText().toString().trim();

                    if (name.length() < 3) {
                        nameInput.setError("Nama minimal 3 karakter");
                        nameInput.requestFocus();
                        return;
                    }

                    if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailInput.setError("Masukkan email yang valid");
                        emailInput.requestFocus();
                        return;
                    }

                    AuthSessionStore.updateProfile(requireContext(), name, email);
                    updateAccountSummary();
                    AppThemeManager.applyToViewTree(getView());
                    showToast("Profil akun diperbarui");
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showChangePasswordDialog() {
        String email = AuthSessionStore.getEmail(requireContext());
        if (TextUtils.isEmpty(email)) {
            showToast("Lengkapi profil akun terlebih dahulu");
            return;
        }

        EditText currentPasswordInput = createDialogInput("Kata sandi saat ini",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText newPasswordInput = createDialogInput("Kata sandi baru",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirmPasswordInput = createDialogInput("Konfirmasi kata sandi baru",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Ubah Kata Sandi")
                .setView(dialogInputContainer(currentPasswordInput, newPasswordInput, confirmPasswordInput))
                .setNegativeButton("Batal", null)
                .setPositiveButton("Simpan", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String currentPassword = currentPasswordInput.getText().toString();
                    String newPassword = newPasswordInput.getText().toString();
                    String confirmPassword = confirmPasswordInput.getText().toString();

                    if (!AuthSessionStore.canSignIn(requireContext(), email, currentPassword)) {
                        currentPasswordInput.setError("Kata sandi saat ini belum cocok");
                        currentPasswordInput.requestFocus();
                        return;
                    }

                    if (newPassword.length() < 6) {
                        newPasswordInput.setError("Kata sandi minimal 6 karakter");
                        newPasswordInput.requestFocus();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        confirmPasswordInput.setError("Konfirmasi kata sandi belum sama");
                        confirmPasswordInput.requestFocus();
                        return;
                    }

                    AuthSessionStore.updatePassword(requireContext(), newPassword);
                    showToast("Kata sandi berhasil diperbarui");
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showLogoutDialog(View sourceView) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Keluar akun?")
                .setMessage("Kamu bisa masuk kembali dengan email dan kata sandi yang sudah terdaftar.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Keluar", (dialog, which) -> {
                    AuthSessionStore.signOut(requireContext());
                    NavController navController = Navigation.findNavController(sourceView);
                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(navController.getGraph().getStartDestinationId(), true)
                            .build();
                    navController.navigate(R.id.navigation_login, null, navOptions);
                })
                .show();
    }

    private EditText createDialogInput(String hint, int inputType) {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(18), dp(6), dp(18), dp(6));
        return input;
    }

    private LinearLayout dialogInputContainer(EditText... inputs) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(6), dp(18), 0);

        for (int i = 0; i < inputs.length; i++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                params.topMargin = dp(10);
            }
            container.addView(inputs[i], params);
        }
        return container;
    }

    private void saveSettings() {
        preferences.edit()
                .putBoolean(KEY_DARK_MODE, switchDarkMode.isChecked())
                .putString(KEY_THEME, selectedTheme)
                .putString(KEY_TEXT_SIZE, selectedTextSize)
                .putBoolean(KEY_DAILY_NOTIFICATION, selectedDailyNotification)
                .putBoolean(KEY_COOKING_REMINDER, selectedCookingReminder)
                .apply();

        if (selectedCookingReminder) {
            boolean scheduled = CookingReminderScheduler.scheduleAll(requireContext());
            if (!scheduled) {
                selectedCookingReminder = false;
                setSwitchesWithoutSaving(selectedDarkMode, selectedDailyNotification, false);
                CookingReminderScheduler.cancelAll(requireContext());
                showToast("Pengingat gagal dijadwalkan di perangkat ini");
                return;
            }
        } else {
            CookingReminderScheduler.cancelAll(requireContext());
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
        reminders.clear();

        setSwitchesWithoutSaving(false, true, false);
        tvThemeValue.setText(selectedTheme);
        tvTextSizeValue.setText(selectedTextSize);
        updateCookingReminderSummary();
        CookingReminderScheduler.cancelAll(requireContext());
        CookingReminderStore.clearReminders(requireContext());

        preferences.edit()
                .putBoolean(KEY_DARK_MODE, false)
                .putString(KEY_THEME, DEFAULT_THEME)
                .putString(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
                .putBoolean(KEY_DAILY_NOTIFICATION, true)
                .putBoolean(KEY_COOKING_REMINDER, false)
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
            reminders = CookingReminderStore.getReminders(requireContext());
            if (reminders.isEmpty()) {
                showCookingReminderDialog();
            } else {
                requestNotificationPermissionIfNeeded();
                preferences.edit().putBoolean(KEY_COOKING_REMINDER, true).apply();
                boolean scheduled = CookingReminderScheduler.scheduleAll(requireContext());
                if (!scheduled) {
                    selectedCookingReminder = false;
                    setSwitchesWithoutSaving(selectedDarkMode, selectedDailyNotification, false);
                    preferences.edit().putBoolean(KEY_COOKING_REMINDER, false).apply();
                    showToast("Pengingat gagal dijadwalkan di perangkat ini");
                    return;
                }
                updateCookingReminderSummary();
                showToast("Semua pengingat memasak diaktifkan");
            }
        } else {
            CookingReminderScheduler.cancelAll(requireContext());
            preferences.edit().putBoolean(KEY_COOKING_REMINDER, false).apply();
            updateCookingReminderSummary();
            showToast("Pengingat memasak dimatikan");
        }
    }

    private void showCookingReminderListDialog() {
        reminders = CookingReminderStore.getReminders(requireContext());
        if (reminders.isEmpty()) {
            showCookingReminderDialog();
            return;
        }

        String[] items = new String[reminders.size() + 1];
        items[0] = "Tambah pengingat baru";
        for (int i = 0; i < reminders.size(); i++) {
            CookingReminderStore.CookingReminder reminder = reminders.get(i);
            items[i + 1] = CookingReminderScheduler.formatTime(reminder.hour, reminder.minute)
                    + " - " + reminder.recipeName;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Jadwal Pengingat Memasak")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showCookingReminderDialog();
                    } else {
                        showReminderOptions(reminders.get(which - 1));
                    }
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void showReminderOptions(CookingReminderStore.CookingReminder reminder) {
        new AlertDialog.Builder(requireContext())
                .setTitle(CookingReminderScheduler.formatTime(reminder.hour, reminder.minute)
                        + " - " + reminder.recipeName)
                .setItems(new String[]{"Ubah pengingat", "Hapus pengingat"}, (dialog, which) -> {
                    if (which == 0) {
                        showCookingReminderDialog(reminder);
                    } else {
                        CookingReminderScheduler.cancel(requireContext(), reminder.id);
                        CookingReminderStore.removeReminder(requireContext(), reminder.id);
                        reminders = CookingReminderStore.getReminders(requireContext());
                        selectedCookingReminder = !reminders.isEmpty() && switchCookingReminder.isChecked();
                        if (reminders.isEmpty()) {
                            preferences.edit().putBoolean(KEY_COOKING_REMINDER, false).apply();
                            setSwitchesWithoutSaving(selectedDarkMode, selectedDailyNotification, false);
                        }
                        updateCookingReminderSummary();
                        showToast("Pengingat dihapus");
                    }
                })
                .show();
    }

    private void showCookingReminderDialog() {
        showCookingReminderDialog(null);
    }

    private void showCookingReminderDialog(CookingReminderStore.CookingReminder existingReminder) {
        int hour = existingReminder == null ? selectedReminderHour : existingReminder.hour;
        int minute = existingReminder == null ? selectedReminderMinute : existingReminder.minute;
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) ->
                        showRecipeNameDialog(selectedHour, selectedMinute, existingReminder),
                hour,
                minute,
                true
        );
        timePickerDialog.setTitle("Pilih jam pengingat");
        timePickerDialog.setOnCancelListener(dialog -> resetReminderIfIncomplete());
        timePickerDialog.show();
    }

    private void showRecipeNameDialog(int hour, int minute, CookingReminderStore.CookingReminder existingReminder) {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setHint("Contoh: Ayam Teriyaki");
        input.setText(existingReminder == null ? "" : existingReminder.recipeName);
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
                    saveAndScheduleCookingReminder(existingReminder);
                })
                .setOnCancelListener(dialog -> resetReminderIfIncomplete())
                .show();
    }

    private void resetReminderIfIncomplete() {
        if (!CookingReminderStore.getReminders(requireContext()).isEmpty()) {
            return;
        }

        selectedCookingReminder = false;
        setSwitchesWithoutSaving(selectedDarkMode, selectedDailyNotification, false);
        updateCookingReminderSummary();
    }

    private void saveAndScheduleCookingReminder(CookingReminderStore.CookingReminder existingReminder) {
        requestNotificationPermissionIfNeeded();
        CookingReminderStore.CookingReminder reminder;
        if (existingReminder == null) {
            reminder = CookingReminderStore.addReminder(
                    requireContext(),
                    selectedReminderHour,
                    selectedReminderMinute,
                    selectedReminderRecipe
            );
        } else {
            reminder = new CookingReminderStore.CookingReminder(
                    existingReminder.id,
                    selectedReminderHour,
                    selectedReminderMinute,
                    selectedReminderRecipe,
                    true
            );
            CookingReminderStore.updateReminder(requireContext(), reminder);
        }

        preferences.edit()
                .putBoolean(KEY_COOKING_REMINDER, true)
                .apply();

        boolean scheduled = CookingReminderScheduler.schedule(requireContext(), reminder);
        if (!scheduled) {
            selectedCookingReminder = false;
            setSwitchesWithoutSaving(selectedDarkMode, selectedDailyNotification, false);
            preferences.edit().putBoolean(KEY_COOKING_REMINDER, false).apply();
            CookingReminderScheduler.cancel(requireContext(), reminder.id);
            if (existingReminder == null) {
                CookingReminderStore.removeReminder(requireContext(), reminder.id);
            }
            updateCookingReminderSummary();
            showToast("Pengingat gagal dijadwalkan di perangkat ini");
            return;
        }
        reminders = CookingReminderStore.getReminders(requireContext());
        updateCookingReminderSummary();
        showToast("Pengingat " + selectedReminderRecipe + " disetel pukul "
                + CookingReminderScheduler.formatTime(selectedReminderHour, selectedReminderMinute));
    }

    private void updateCookingReminderSummary() {
        if (tvCookingReminderSummary == null) {
            return;
        }

        reminders = CookingReminderStore.getReminders(requireContext());
        if (selectedCookingReminder && !reminders.isEmpty()) {
            tvCookingReminderSummary.setText(CookingReminderStore.summary(reminders));
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
