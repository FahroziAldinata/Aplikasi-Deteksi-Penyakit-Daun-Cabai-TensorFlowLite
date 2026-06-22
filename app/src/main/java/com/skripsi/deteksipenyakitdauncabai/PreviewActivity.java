package com.skripsi.deteksipenyakitdauncabai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;

public class PreviewActivity extends AppCompatActivity {

    private Uri imageUri;

    /**
     * Dipanggil saat activity dibuat.
     * Mengambil URI gambar dari Intent, menampilkan pratinjau gambar menggunakan Glide,
     * serta mengatur tombol deteksi untuk membuka ResultActivity dengan URI gambar tersebut.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        imageUri = getIntent().getData();
        ImageView imageView = findViewById(R.id.imageView);
        Button btnDetect = findViewById(R.id.btnDetect);

        if (imageUri != null) {
            Glide.with(this)
                    .load(imageUri)
                    .transform(new FitCenter())
                    .into(imageView);
        } else {
            Toast.makeText(this, "Gagal memuat gambar!", Toast.LENGTH_SHORT).show();
        }

        btnDetect.setOnClickListener(v -> {
            if (imageUri != null) {
                Intent intent = new Intent(this, ResultActivity.class);
                intent.setData(imageUri);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Gambar belum tersedia!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}