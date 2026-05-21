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

public class LoginFragment extends Fragment {

    private EditText emailInput;
    private EditText passwordInput;

    public LoginFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        emailInput = view.findViewById(R.id.etLoginEmail);
        passwordInput = view.findViewById(R.id.etLoginPassword);

        view.findViewById(R.id.btnLoginSubmit).setOnClickListener(v -> handleLogin(v));
        view.findViewById(R.id.btnLoginCreateAccount).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_register));
        view.findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Fitur lupa kata sandi belum tersedia", Toast.LENGTH_SHORT).show());

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

    private void handleLogin(View view) {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

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

        if (!AuthSessionStore.hasRegisteredAccount(requireContext())) {
            Toast.makeText(requireContext(), "Daftar akun dulu untuk mulai menyimpan resep", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigate(R.id.navigation_register);
            return;
        }

        if (!AuthSessionStore.canSignIn(requireContext(), email, password)) {
            passwordInput.setError("Email atau kata sandi belum cocok");
            passwordInput.requestFocus();
            return;
        }

        AuthSessionStore.signIn(requireContext());
        Toast.makeText(requireContext(), "Selamat datang kembali, "
                + AuthSessionStore.getDisplayName(requireContext()), Toast.LENGTH_SHORT).show();
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
