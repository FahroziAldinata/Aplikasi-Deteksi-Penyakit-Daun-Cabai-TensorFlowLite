package com.skripsi.deteksipenyakitdauncabai;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import android.graphics.drawable.Drawable;

public class OverlayView extends View {

    private List<BoundingBox> results = new ArrayList<>();
    private final Paint boxPaint  = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint   = new Paint();

    private ImageView imageView;

    /**
     * Konstruktor yang dipanggil saat OverlayView dibuat dari XML layout.
     * Menginisialisasi semua objek Paint yang digunakan untuk menggambar
     * bounding box, teks label, dan background label.
     *
     * @param context Konteks aplikasi Android.
     * @param attrs   Atribut dari XML layout.
     */
    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints(context);
    }

    /**
     * Menyimpan referensi ImageView yang menampilkan gambar.
     * Digunakan untuk membaca matrix transformasi fitCenter agar posisi
     * bounding box sesuai dengan posisi gambar yang ditampilkan.
     *
     * @param imageView ImageView tempat gambar ditampilkan.
     */
    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    /**
     * Menginisialisasi gaya Paint untuk bounding box, teks label, dan background label.
     *
     * @param context Konteks aplikasi untuk mengambil warna dari resources.
     */
    private void initPaints(Context context) {
        boxPaint.setColor(ContextCompat.getColor(context, R.color.bounding_box_color));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(42f);
        textPaint.setAntiAlias(true);

        bgPaint.setColor(Color.BLACK);
        bgPaint.setAlpha(160);
    }

    /**
     * Menghapus semua hasil deteksi dan memperbarui tampilan.
     * Dipanggil ketika ingin mereset overlay tanpa hasil deteksi.
     */
    public void clear() {
        results.clear();
        invalidate();
    }

    /**
     * Menerima daftar BoundingBox hasil deteksi dan memicu proses menggambar ulang.
     *
     * @param boxes Daftar BoundingBox yang akan ditampilkan di atas gambar.
     */
    public void setResults(List<BoundingBox> boxes) {
        this.results = boxes;
        invalidate();
    }

    /**
     * Menggambar semua bounding box dan label hasil deteksi di atas canvas.
     * Koordinat relatif (0–1) dari setiap BoundingBox dikonversi ke koordinat
     * pixel menggunakan matrix transformasi fitCenter dari ImageView.
     * Posisi box dan label di-clamp agar tidak melewati batas area gambar dan canvas.
     *
     * @param canvas Canvas tempat bounding box dan label digambar.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (imageView == null || results.isEmpty()) return;

        float[] matrixValues = new float[9];
        imageView.getImageMatrix().getValues(matrixValues);

        float scaleX = matrixValues[Matrix.MSCALE_X];
        float scaleY = matrixValues[Matrix.MSCALE_Y];
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];

        Drawable drawable = imageView.getDrawable();
        if (drawable == null) return;
        int intrinsicWidth  = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        float canvasWidth  = (float) getWidth();
        float canvasHeight = (float) getHeight();

        float imageDisplayWidth  = intrinsicWidth  * scaleX;
        float imageDisplayHeight = intrinsicHeight * scaleY;

        float imageRight  = transX + imageDisplayWidth;
        float imageBottom = transY + imageDisplayHeight;

        for (BoundingBox box : results) {

            float left   = box.x1 * imageDisplayWidth  + transX;
            float top    = box.y1 * imageDisplayHeight + transY;
            float right  = box.x2 * imageDisplayWidth  + transX;
            float bottom = box.y2 * imageDisplayHeight + transY;

            left   = Math.max(left,   transX);
            top    = Math.max(top,    transY);
            right  = Math.min(right,  imageRight);
            bottom = Math.min(bottom, imageBottom);

            canvas.drawRect(left, top, right, bottom, boxPaint);

            String label      = box.clsName + " (" + String.format("%.2f", box.cnf) + ")";
            float  textWidth  = textPaint.measureText(label);
            float  textHeight = textPaint.getTextSize();

            float labelPadX = 10f;
            float labelPadY = 6f;
            float bgWidth   = textWidth  + labelPadX * 2;
            float bgHeight  = textHeight + labelPadY * 2;

            float labelLeft = left;
            float labelTop  = top - bgHeight;

            if (labelTop < transY) {
                labelTop = top;
            }

            if (labelLeft + bgWidth > canvasWidth) {
                labelLeft = canvasWidth - bgWidth;
            }
            labelLeft = Math.max(labelLeft, transX);

            if (labelTop + bgHeight > canvasHeight) {
                labelTop = canvasHeight - bgHeight;
            }

            canvas.drawRect(
                    labelLeft,
                    labelTop,
                    labelLeft + bgWidth,
                    labelTop + bgHeight,
                    bgPaint
            );

            canvas.drawText(
                    label,
                    labelLeft + labelPadX,
                    labelTop  + textHeight,
                    textPaint
            );
        }
    }
}