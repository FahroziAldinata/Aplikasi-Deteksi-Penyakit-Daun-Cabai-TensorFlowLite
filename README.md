# Deteksi Penyakit Daun Cabai

Aplikasi Android untuk mendeteksi penyakit pada daun cabai menggunakan model deep learning (YOLO, format TFLite) yang dijalankan secara on-device (offline) dengan TensorFlow Lite.

## Deskripsi

Aplikasi ini memungkinkan pengguna untuk mengambil foto daun cabai (melalui kamera atau galeri), kemudian sistem akan mendeteksi dan mengklasifikasikan kondisi daun ke dalam salah satu dari 4 kelas:

| Label | Keterangan |
|---|---|
| `Sehat` | Daun dalam kondisi sehat |
| `Bercak_Daun` | Penyakit bercak daun |
| `Thrips` | Hama Thrips |
| `Virus_Kuning` | Penyakit virus kuning |

Hasil deteksi ditampilkan berupa **bounding box** di atas gambar beserta label kelas dan nilai *confidence*, lengkap dengan waktu inferensi model.

## Fitur

- 📷 Ambil foto langsung dari kamera
- 🖼️ Pilih gambar dari galeri
- 🔍 Deteksi objek (penyakit daun cabai) secara on-device menggunakan TensorFlow Lite
- 📦 Visualisasi hasil deteksi dengan bounding box dan label confidence di atas gambar
- ⏱️ Menampilkan waktu inferensi model
- 🌓 Mendukung mode terang dan gelap (Day/Night theme)

## Arsitektur & Alur Aplikasi

```
MainActivity  ──(pilih/ambil foto)──>  PreviewActivity  ──(klik Deteksi)──>  ResultActivity
```

1. **MainActivity** — Halaman utama berisi dua tombol: "Pilih Gambar" (galeri) dan "Ambil Foto" (kamera).
2. **PreviewActivity** — Menampilkan pratinjau gambar yang dipilih/diambil sebelum diproses, dengan tombol "Deteksi".
3. **ResultActivity** — Menjalankan model deteksi pada gambar (di-resize ke 640x640), menampilkan hasil berupa jumlah objek terdeteksi, waktu inferensi, serta bounding box melalui `OverlayView`.

## Struktur Komponen Utama

| File | Fungsi |
|---|---|
| `MainActivity.java` | Entry point, menangani pemilihan gambar (kamera/galeri) |
| `PreviewActivity.java` | Menampilkan pratinjau gambar sebelum deteksi |
| `ResultActivity.java` | Menjalankan deteksi dan menampilkan hasil |
| `Detector.java` | Wrapper TensorFlow Lite — load model, preprocessing, inferensi, parsing output, dan Non-Maximum Suppression (NMS) |
| `BoundingBox.java` | Model data hasil deteksi (koordinat, confidence, kelas) |
| `OverlayView.java` | Custom View untuk menggambar bounding box & label di atas gambar |
| `model.tflite` *(assets)* | Model deteksi (tidak disertakan di repo ini — perlu ditambahkan secara manual) |
| `labels.txt` *(assets)* | Daftar nama kelas/label |

## Teknologi yang Digunakan

- **Bahasa**: Java
- **Min SDK**: 29 (Android 10) — **Target SDK**: 35
- **Build System**: Gradle 8.11.1, Android Gradle Plugin 8.9.1
- **Library utama**:
  - `org.tensorflow:tensorflow-lite:2.12.0` — inferensi model AI
  - `org.tensorflow:tensorflow-lite-support:0.4.3` — preprocessing image tensor
  - `com.github.bumptech.glide:glide:4.16.0` — load & tampilkan gambar
  - AndroidX: `appcompat`, `activity`, `constraintlayout`
  - `com.google.android.material:material` — komponen UI (MaterialButton, dll)

## Konfigurasi Model

- Ukuran input gambar: **640x640**
- Normalisasi: mean `0`, std `255` (rentang nilai piksel 0–1)
- Threshold confidence: `0.2`
- Threshold IoU (NMS): `0.2`
- Output diproses dengan algoritma **Non-Maximum Suppression (NMS)** untuk menghapus deteksi duplikat/tumpang tindih

## Cara Menjalankan Proyek

### Prasyarat
- Android Studio (terbaru direkomendasikan)
- JDK 21
- Perangkat/emulator dengan Android 10 (API 29) atau lebih baru
- File `model.tflite` hasil training (letakkan di `app/src/main/assets/`)

### Langkah
1. Clone repository ini
2. Buka proyek dengan Android Studio
3. Pastikan file `model.tflite` sudah ada di folder `app/src/main/assets/` (sejajar dengan `labels.txt`)
4. Sinkronkan Gradle (`Sync Now`)
5. Jalankan aplikasi pada perangkat fisik atau emulator (disarankan perangkat fisik untuk akses kamera)

```bash
./gradlew assembleDebug
```

### Izin (Permissions)
Aplikasi memerlukan izin berikut:
- `android.permission.CAMERA` — untuk mengambil foto
- `android.permission.WRITE_EXTERNAL_STORAGE` — untuk menyimpan foto sementara

## Catatan
- Opsi `noCompress "tflite"` sudah diatur di `build.gradle` agar file model tidak ikut terkompresi saat proses build (penting agar `Interpreter` TFLite dapat membaca file dengan benar via memory-mapping).
- Proyek ini merupakan bagian dari **Skripsi** (tugas akhir).

## Lisensi & Kredit

© 2026 Fahrozi Aldinata — Skripsi
