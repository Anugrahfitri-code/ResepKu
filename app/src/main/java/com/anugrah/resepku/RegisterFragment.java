package com.anugrah.resepku;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

public class RegisterFragment extends Fragment {

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    public RegisterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);
        nameInput = view.findViewById(R.id.etRegisterName);
        emailInput = view.findViewById(R.id.etRegisterEmail);
        passwordInput = view.findViewById(R.id.etRegisterPassword);
        confirmPasswordInput = view.findViewById(R.id.etRegisterConfirmPassword);

        view.findViewById(R.id.btnRegisterSubmit).setOnClickListener(v -> handleRegister(v));
        view.findViewById(R.id.btnRegisterLogin).setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            if (!navController.popBackStack()) {
                navController.navigate(R.id.navigation_login);
            }
        });

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

    private void handleRegister(View view) {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

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

        if (password.length() < 6) {
            passwordInput.setError("Kata sandi minimal 6 karakter");
            passwordInput.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Konfirmasi kata sandi belum sama");
            confirmPasswordInput.requestFocus();
            return;
        }

        AuthSessionStore.register(requireContext(), name, email, password);
        Toast.makeText(requireContext(), "Akun siap, selamat memasak " + name, Toast.LENGTH_SHORT).show();
        openHome(view);
    }

    private void openHome(View view) {
        NavController navController = Navigation.findNavController(view);
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.navigation_login, true)
                .build();
        navController.navigate(R.id.navigation_home, null, navOptions);
    }
}
