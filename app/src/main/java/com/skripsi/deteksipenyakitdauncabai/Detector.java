package com.skripsi.deteksipenyakitdauncabai;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Detector {

    private static final String TAG = "Detector";

    private Interpreter interpreter;
    private final List<String> labels = new ArrayList<>();

    private int tensorWidth = 0;
    private int tensorHeight = 0;
    private int numChannel = 0;
    private int numElements = 0;

    private static final float INPUT_MEAN    = 0f;
    private static final float INPUT_STD     = 255f;
    private static final DataType INPUT_IMAGE_TYPE  = DataType.FLOAT32;
    private static final DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;

    private static final float CONF_THRESHOLD = 0.2f;
    private static final float IOU_THRESHOLD  = 0.2f;

    private final ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STD))
            .add(new CastOp(INPUT_IMAGE_TYPE))
            .build();

    private final Context context;
    private final String modelPath;
    private final String labelPath;

    /**
     * Konstruktor untuk menginisialisasi Detector dengan konteks aplikasi,
     * path file model TFLite, dan path file label kelas.
     *
     * @param context   Konteks aplikasi Android.
     * @param modelPath Nama file model TFLite di folder assets.
     * @param labelPath Nama file label kelas di folder assets.
     */
    public Detector(Context context, String modelPath, String labelPath) {
        this.context   = context;
        this.modelPath = modelPath;
        this.labelPath = labelPath;
    }

    /**
     * Memuat model TFLite dari assets dan membaca daftar label kelas.
     * Membaca bentuk tensor input dan output untuk menentukan ukuran
     * gambar yang dibutuhkan model serta jumlah channel dan elemen output.
     *
     * @throws IOException jika file model atau label gagal dibaca.
     */
    public void setup() throws IOException {
        MappedByteBuffer model = FileUtil.loadMappedFile(context, modelPath);

        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(model, options);

        int[] inputShape  = interpreter.getInputTensor(0).shape();
        int[] outputShape = interpreter.getOutputTensor(0).shape();

        tensorWidth  = inputShape[1];
        tensorHeight = inputShape[2];
        numChannel   = outputShape[1];
        numElements  = outputShape[2];

        Log.d(TAG, "Model loaded — input: " + tensorWidth + "x" + tensorHeight
                + ", output: [" + numChannel + " x " + numElements + "]");

        try (InputStream inputStream = context.getAssets().open(labelPath);
             BufferedReader reader   = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                labels.add(line);
            }
        }

        Log.d(TAG, "Labels loaded: " + labels.size() + " kelas");
    }

    /**
     * Melepaskan resource Interpreter TFLite setelah proses deteksi selesai.
     * Wajib dipanggil untuk mencegah memory leak.
     */
    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    /**
     * Menjalankan inferensi model deteksi pada bitmap yang diberikan.
     * Gambar di-resize ke ukuran input model, diproses melalui ImageProcessor,
     * lalu dijalankan pada interpreter TFLite. Hasil output diparse menjadi
     * daftar BoundingBox yang sudah melalui proses NMS.
     *
     * @param bitmap Gambar yang akan dideteksi.
     * @return Daftar BoundingBox hasil deteksi, atau list kosong jika tidak ada objek.
     */
    public List<BoundingBox> detect(Bitmap bitmap) {
        if (interpreter == null) {
            Log.w(TAG, "detect() dipanggil sebelum setup()");
            return new ArrayList<>();
        }
        if (tensorWidth == 0 || tensorHeight == 0) {
            Log.w(TAG, "Ukuran tensor belum terbaca");
            return new ArrayList<>();
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidth, tensorHeight, true);

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(resizedBitmap);
        TensorImage processedImage = imageProcessor.process(tensorImage);

        TensorBuffer output = TensorBuffer.createFixedSize(
                new int[]{1, numChannel, numElements},
                OUTPUT_IMAGE_TYPE
        );

        interpreter.run(processedImage.getBuffer(), output.getBuffer().rewind());

        float[] outputArray = output.getFloatArray();

        List<BoundingBox> boxes = bestBox(outputArray);

        if (boxes == null || boxes.isEmpty()) {
            Log.d(TAG, "Tidak ada objek terdeteksi");
            return new ArrayList<>();
        }

        Log.d(TAG, "Terdeteksi " + boxes.size() + " objek setelah NMS");
        return boxes;
    }

    /**
     * Mem-parsing array output model menjadi daftar BoundingBox.
     * Setiap elemen dicek confidence-nya terhadap threshold, lalu koordinat
     * bounding box dihitung dari format center-x, center-y, width, height.
     * Koordinat yang melewati batas gambar di-clamp ke rentang [0, 1]
     * agar objek di tepi gambar tetap terdeteksi. Hasil akhir diproses
     * melalui NMS untuk menghilangkan box yang saling tumpang tindih.
     *
     * @param array Array float output mentah dari model TFLite.
     * @return Daftar BoundingBox setelah filtering dan NMS, atau null jika kosong.
     */
    private List<BoundingBox> bestBox(float[] array) {
        List<BoundingBox> boxes = new ArrayList<>();

        for (int c = 0; c < numElements; c++) {

            float maxConf = -1.0f;
            int   maxIdx  = -1;
            int   j        = 4;
            int   arrayIdx = c + numElements * j;

            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx];
                    maxIdx  = j - 4;
                }
                j++;
                arrayIdx += numElements;
            }

            if (maxConf <= CONF_THRESHOLD) continue;
            if (maxIdx < 0 || maxIdx >= labels.size()) continue;

            String clsName = labels.get(maxIdx);

            float cx = array[c];
            float cy = array[c + numElements];
            float w  = array[c + numElements * 2];
            float h  = array[c + numElements * 3];

            float rawX1 = cx - (w / 2f);
            float rawY1 = cy - (h / 2f);
            float rawX2 = cx + (w / 2f);
            float rawY2 = cy + (h / 2f);

            float x1 = Math.max(0f, rawX1);
            float y1 = Math.max(0f, rawY1);
            float x2 = Math.min(1f, rawX2);
            float y2 = Math.min(1f, rawY2);

            if (x2 <= x1 || y2 <= y1) continue;

            float clampedW = x2 - x1;
            float clampedH = y2 - y1;

            boxes.add(new BoundingBox(
                    x1, y1, x2, y2,
                    cx, cy,
                    clampedW, clampedH,
                    maxConf, maxIdx, clsName
            ));
        }

        if (boxes.isEmpty()) return null;
        return applyNMS(boxes);
    }

    /**
     * Menerapkan algoritma Non-Maximum Suppression (NMS) pada daftar BoundingBox.
     * Box diurutkan dari confidence tertinggi, lalu box yang tumpang tindih
     * melebihi IOU_THRESHOLD dengan box terpilih akan dihapus.
     *
     * @param boxes Daftar BoundingBox sebelum NMS.
     * @return Daftar BoundingBox setelah duplikasi dihilangkan.
     */
    private List<BoundingBox> applyNMS(List<BoundingBox> boxes) {
        List<BoundingBox> sorted = new ArrayList<>(boxes);
        sorted.sort((b1, b2) -> Float.compare(b2.cnf, b1.cnf));

        List<BoundingBox> selected = new ArrayList<>();

        while (!sorted.isEmpty()) {
            BoundingBox best = sorted.remove(0);
            selected.add(best);

            sorted.removeIf(box -> calculateIoU(best, box) >= IOU_THRESHOLD);
        }

        return selected;
    }

    /**
     * Menghitung nilai Intersection over Union (IoU) antara dua BoundingBox.
     * Digunakan oleh NMS untuk mengukur seberapa besar dua box saling tumpang tindih.
     * Nilai 0 berarti tidak ada tumpang tindih, nilai 1 berarti identik.
     *
     * @param b1 BoundingBox pertama.
     * @param b2 BoundingBox kedua.
     * @return Nilai IoU antara 0 dan 1.
     */
    private float calculateIoU(BoundingBox b1, BoundingBox b2) {
        float interX1 = Math.max(b1.x1, b2.x1);
        float interY1 = Math.max(b1.y1, b2.y1);
        float interX2 = Math.min(b1.x2, b2.x2);
        float interY2 = Math.min(b1.y2, b2.y2);

        float interArea = Math.max(0f, interX2 - interX1)
                * Math.max(0f, interY2 - interY1);

        float area1 = b1.w * b1.h;
        float area2 = b2.w * b2.h;

        if (area1 + area2 - interArea == 0f) return 0f;

        return interArea / (area1 + area2 - interArea);
    }
}