package com.skripsi.deteksipenyakitdauncabai;

/**
 * Model data yang merepresentasikan satu bounding box hasil deteksi objek.
 * Menyimpan koordinat posisi, ukuran, nilai confidence, dan nama kelas objek.
 */
public class BoundingBox {
    public float x1;       // Koordinat kiri box (ternormalisasi 0–1)
    public float y1;       // Koordinat atas box (ternormalisasi 0–1)
    public float x2;       // Koordinat kanan box (ternormalisasi 0–1)
    public float y2;       // Koordinat bawah box (ternormalisasi 0–1)
    public float cx;       // Koordinat center-x box (ternormalisasi 0–1)
    public float cy;       // Koordinat center-y box (ternormalisasi 0–1)
    public float w;        // Lebar box (ternormalisasi 0–1)
    public float h;        // Tinggi box (ternormalisasi 0–1)
    public float cnf;      // Nilai confidence deteksi (0–1)
    public int cls;        // Indeks kelas objek
    public String clsName; // Nama kelas objek

    /**
     * Membuat instance BoundingBox dengan seluruh atribut posisi,
     * ukuran, confidence, dan informasi kelas objek yang terdeteksi.
     *
     * @param x1      Koordinat kiri box (ternormalisasi 0–1).
     * @param y1      Koordinat atas box (ternormalisasi 0–1).
     * @param x2      Koordinat kanan box (ternormalisasi 0–1).
     * @param y2      Koordinat bawah box (ternormalisasi 0–1).
     * @param cx      Koordinat center-x box (ternormalisasi 0–1).
     * @param cy      Koordinat center-y box (ternormalisasi 0–1).
     * @param w       Lebar box (ternormalisasi 0–1).
     * @param h       Tinggi box (ternormalisasi 0–1).
     * @param cnf     Nilai confidence deteksi (0–1).
     * @param cls     Indeks kelas objek.
     * @param clsName Nama kelas objek.
     */
    public BoundingBox(float x1, float y1, float x2, float y2,
                       float cx, float cy, float w, float h,
                       float cnf, int cls, String clsName) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.cx = cx;
        this.cy = cy;
        this.w = w;
        this.h = h;
        this.cnf = cnf;
        this.cls = cls;
        this.clsName = clsName;
    }
}