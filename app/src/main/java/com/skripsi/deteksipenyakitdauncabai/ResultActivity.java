package com.skripsi.deteksipenyakitdauncabai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.util.List;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    /**
     * Dipanggil saat activity dibuat.
     * Mengambil URI gambar dari Intent, memuat dan mengubah ukuran gambar menjadi 640x640,
     * menjalankan model deteksi TFLite, lalu menampilkan jumlah objek terdeteksi
     * beserta waktu inferensi ke layar. Bounding box hasil deteksi ditampilkan
     * melalui OverlayView di atas gambar.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ImageView   imageView   = findViewById(R.id.imageViewResult);
        TextView    textResult  = findViewById(R.id.textResult);
        OverlayView overlayView = findViewById(R.id.overlayView);
        Button      btnKembali  = findViewById(R.id.btnkembali);

        btnKembali.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        Uri imageUri = getIntent().getData();

        if (imageUri != null) {
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {

                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
                if (originalBitmap == null) {
                    textResult.setText(R.string.failed_load_image);
                    return;
                }

                Bitmap bitmap = Bitmap.createScaledBitmap(originalBitmap, 640, 640, true);
                imageView.setImageBitmap(bitmap);
                overlayView.setImageView(imageView);

                long startTime = SystemClock.elapsedRealtime();
                Detector detector = new Detector(this, "model.tflite", "labels.txt");
                detector.setup();
                List<BoundingBox> results = detector.detect(bitmap);
                detector.close();
                long inferenceTime = SystemClock.elapsedRealtime() - startTime;

                Log.d(TAG, "Inference time: " + inferenceTime + " ms");

                String summary = getString(R.string.detection_found, results.size())
                        + " | Waktu: " + inferenceTime + " ms";

                textResult.setText(summary);
                overlayView.setResults(results);

            } catch (Exception e) {
                Log.e(TAG, "Error deteksi gambar", e);
                textResult.setText(R.string.detection_failed);
            }
        } else {
            textResult.setText(R.string.failed_load_image);
        }
    }
}