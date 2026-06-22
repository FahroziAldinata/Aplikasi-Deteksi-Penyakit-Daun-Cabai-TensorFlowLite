package com.skripsi.deteksipenyakitdauncabai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Uri photoUri;
    private File photoFile;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> takePhotoLauncher;

    /**
     * Dipanggil pertama kali saat activity dibuat.
     * Menginisialisasi tampilan, mendaftarkan launcher untuk galeri dan kamera,
     * serta mengatur aksi pada tombol pilih gambar dan ambil foto.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedUri = result.getData().getData();
                        openPreviewActivity(selectedUri);
                    } else {
                        Toast.makeText(this, "Tidak ada gambar yang dipilih", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (photoUri != null) {
                            openPreviewActivity(photoUri);
                        } else {
                            Toast.makeText(this, "URI foto tidak ditemukan!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Foto dibatalkan", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        Button btnSelect = findViewById(R.id.btnSelectImage);
        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        Button btnTake = findViewById(R.id.btnTakePhoto);
        btnTake.setOnClickListener(v -> openCamera());
    }

    /**
     * Membuka kamera perangkat untuk mengambil foto baru.
     * Membuat file sementara sebagai tujuan penyimpanan foto,
     * kemudian meluncurkan intent kamera dengan URI file tersebut.
     * Jika kamera tidak tersedia atau file gagal dibuat, menampilkan pesan error.
     */
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.e(TAG, "Error creating image file", e);
                Toast.makeText(this, "Gagal membuat file foto", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile
                );

                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePhotoLauncher.launch(intent);
            }
        } else {
            Toast.makeText(this, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Membuka PreviewActivity dan mengirimkan URI gambar yang telah dipilih atau difoto.
     * URI dikirim melalui Intent agar PreviewActivity dapat menampilkan pratinjau gambar.
     * Jika URI null, menampilkan pesan gagal memuat gambar.
     *
     * @param uri URI gambar yang akan dikirim ke PreviewActivity.
     */
    private void openPreviewActivity(Uri uri) {
        if (uri != null) {
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.setData(uri);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Membuat file kosong bertipe .jpg di folder Pictures penyimpanan eksternal aplikasi.
     * Nama file menggunakan timestamp agar unik setiap kali foto diambil.
     * File ini digunakan sebagai tujuan penyimpanan hasil foto dari kamera.
     *
     * @return File kosong yang siap digunakan sebagai output kamera.
     * @throws IOException jika proses pembuatan file gagal.
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir != null && !storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            Log.d(TAG, "Directory dibuat: " + created);
        }

        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
}