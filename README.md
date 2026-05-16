# ResepKu - Aplikasi Penjelajah Resep Kuliner

[![Android Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com)

## 📝 Deskripsi Aplikasi
ResepKu adalah aplikasi Android berbasis Java yang dirancang untuk membantu pengguna mencari, menjelajahi, dan menyimpan berbagai resep makanan dan minuman favorit secara praktis. Aplikasi ini dikembangkan untuk memenuhi tugas Final Lab Mobile 2026 dengan fokus pada efisiensi performa, antarmuka yang intuitif, dan kemampuan akses data luring (offline handling).

---

## 🚀 Fitur Utama
* **Jelajah Resep Terpopuler:** Menampilkan daftar resep terkini langsung dari API eksternal menggunakan komponen berkinerja tinggi.
* **Pencarian & Kategori:** Memudahkan pengguna memfilter resep berdasarkan kategori seperti Breakfast, Dessert, Seafood, dll.
* **Simpan Favorit (Akses Offline):** Pengguna dapat menyimpan resep ke penyimpanan lokal untuk diakses kembali kapan saja tanpa koneksi internet.
* **Manajemen Tema:** Mendukung Mode Gelap (Dark Theme) dan Mode Terang (Light Theme) untuk kenyamanan visual maksimal.

---

## 🛠️ Implementasi Teknis & Arsitektur
Aplikasi ini dibangun dengan memenuhi seluruh spesifikasi teknis dasar berikut:
1. **Architecture Components:** Menggunakan `MainActivity` sebagai launcher utama yang mengelola navigasi tiga fragment (`HomeFragment`, `FavoriteFragment`, `SettingFragment`) memanfaatkan **Navigation Component**.
2. **Asynchronous Networking:** Implementasi **Retrofit** untuk mengambil data JSON secara real-time.
3. **Multi-Threading:** Menggunakan `Executor` / `Handler` untuk memproses operasi database di latar belakang (*background thread*) agar UI tetap responsif.
4. **Local Persistence:** Memanfaatkan **SQLite** untuk menyimpan resep favorit dan **SharedPreferences** untuk menyimpan preferensi tema pengguna.
5. **Robust UI/UX:** Dilengkapi mekanisme penanganan kegagalan jaringan berupa tombol *Refresh* (Coba Lagi).

---

## 📱 Cara Penggunaan
1. Buka aplikasi, Anda akan disambut oleh *Splash Screen* singkat.
2. Pada halaman **Beranda**, jelajahi resep kuliner atau gunakan fitur pencarian di bagian atas.
3. Klik salah satu item resep untuk berpindah ke halaman **Detail Resep** guna melihat bahan-bahan dan langkah memasak.
4. Tekan tombol **Simpan ke Favorit** untuk menyimpannya ke database lokal.
5. Buka halaman **Favorit** untuk melihat daftar resep yang telah Anda simpan (tetap berfungsi meskipun perangkat offline).
6. Ubah preferensi visual Anda melalui switch mode gelap di halaman **Pengaturan**.
