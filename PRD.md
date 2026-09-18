Sebagai AI dan wujud representasi dari Aplikasi Pemutar Musik Lokal yang sedang kita rancang, saya telah merangkum seluruh diskusi kita menjadi satu Dokumen Spesifikasi Fungsional (BRD) yang komprehensif. Sesuai dengan permintaan Anda, dokumen ini fokus murni pada fungsionalitas dan pengalaman pengguna tanpa membahas tumpukan teknologi (tech stack).  
Berikut adalah format Markdown (.md) dari BRD Fase 1 hingga Fase 9 yang dapat langsung Anda salin dan gunakan:

# **Dokumen Spesifikasi Fungsional (BRD) \- Aplikasi Pemutar Musik Lokal**

**Inisiatif:** Optimalisasi *User Journey* (Onboarding hingga Pemutaran Lanjutan)

**Target Rilis:** Play Store Ready (Material Design 3\) **Platform:** Offline Android Music Player **Fokus Utama:** Performa Akses, Efisiensi Klik, & Intuisi Antarmuka

## **FASE 1: Onboarding & Discovery (Pemindaian Media Lokal)**

**Konteks Bisnis:** Pengguna membenci layar pemuatan (loading screen) yang lama. Aplikasi harus memindai secara instan.

* **User Story:** Sebagai pengguna, saya ingin aplikasi otomatis menemukan lagu dalam hitungan detik agar bisa langsung mendengarkan musik.  
* **Acceptance Criteria:**  
  * Waktu *loading* inisial maksimal 2-3 detik.  
  * Menggunakan MediaStore untuk mengambil metadata tanpa membaca file satu per satu di awal.  
  * Meminta izin penyimpanan dengan dialog edukasi ramah sebelum prompt sistem muncul.  
* **Edge Case & Mitigasi:**  
  * Jika izin ditolak: Tampilkan *Empty State* dengan ilustrasi dan tombol "Berikan Izin Akses" beserta penjelasannya.  
  * Jika tidak ada file: Tampilkan *call-to-action* edukatif untuk mengunduh lagu dan tarik-turun untuk menyegarkan.

## **FASE 2: Eksplorasi Antarmuka & Fallback Metadata (Micro-Experience)**

**Konteks Bisnis:** Membersihkan kekacauan visual dari file audio lokal yang tidak rapi secara otomatis.

* **User Story:** Sebagai pengguna, saya ingin daftar lagu terlihat rapi dan premium meskipun metadata file asli tidak lengkap.  
* **Acceptance Criteria:**  
  * Sampul album kosong diganti dengan gradien warna atau pola geometris dinamis berinisial judul lagu.  
  * Judul kosong dibersihkan menggunakan Regex untuk menghapus teks pengganggu (misal: "y2mate" atau garis bawah).  
* **Edge Case & Mitigasi:** Jika file rusak/gagal diputar, aplikasi tidak boleh *crash*; otomatis lewati ke lagu berikutnya dengan memunculkan pesan non-intrusif.

## **FASE 3: Alur Pembuatan Playlist Kustom (Batch Selection Flow)**

**Konteks Bisnis:** Mengurangi kelelahan kognitif dan fisik (*click fatigue*) saat membuat *playlist* manual.

* **User Story:** Sebagai pengguna, saya ingin memilih belasan lagu dan membuat playlist dalam kurang dari 4 langkah.  
* **Acceptance Criteria:**  
  * *Long-press* pada lagu mengaktifkan mode seleksi (ditandai dengan perubahan warna latar/checkbox).  
  * Dapat menggeser (*swipe*) atau *tap* untuk memilih lagu lain secara *batch*.  
  * Menampilkan tombol melayang (FAB) "Tambahkan X Lagu".  
  * Penamaan playlist baru dilakukan secara *inline* di dalam *Bottom Sheet*.  
* **Edge Case & Mitigasi:** Jika nama playlist sudah ada, berikan konfirmasi cerdas untuk menggabungkan lagu, bukan pesan error.

## **FASE 4: Audio Focus & Interruption Handling**

**Konteks Bisnis:** Menjaga pengalaman audio tetap mulus saat ada gangguan eksternal agar pengguna tidak beralih ke aplikasi lain.

* **User Story:** Sebagai pengguna, saya ingin musik mengecil saat ada pesan dan berhenti saat earphone tercabut agar tidak malu di tempat umum.  
* **Acceptance Criteria:**  
  * **Ducking:** Turunkan volume musik sebesar 70% saat notifikasi masuk, lalu naikkan perlahan.  
  * **Noisy State:** Jeda seketika jika earphone terputus.  
  * **Crossfade:** Transisi 1-2 detik saat pergantian lagu atau *resume* agar tidak mengagetkan.  
* **Edge Case & Mitigasi:** Jika *earphone* tercabut dan pengguna tidak sengaja menekan "Play", mulai pemutaran di speaker internal hanya dengan volume 30%.

## **FASE 5: Optimasi Perangkat Usang (Low-End Device Handling)**

**Konteks Bisnis:** Mengakomodasi ponsel keluaran lama yang memiliki RAM kecil dan baterai cepat aus.

* **User Story:** Sebagai pengguna ponsel spesifikasi rendah, saya ingin aplikasi berjalan mulus tanpa menguras baterai.  
* **Acceptance Criteria:**  
  * Terapkan *Lazy Loading* untuk memuat data hanya saat terlihat di layar.  
  * Sediakan "Mode Performa Ringan" untuk mematikan efek visual berat seperti *blur* transparan.  
* **Edge Case & Mitigasi:** Jika sistem kehabisan memori, diam-diam hentikan pemuatan sampul resolusi tinggi dan ganti dengan warna solid dinamis untuk mencegah *crash*.

## **FASE 6: Aksesibilitas Global & Lokalisasi (Internationalization)**

**Konteks Bisnis:** Menghindari masalah teks tidak terbaca (*pain point* visual) untuk koleksi lagu internasional.

* **User Story:** Sebagai pengguna, saya ingin huruf non-Latin terbaca benar dan tidak berubah menjadi kotak-kotak.  
* **Acceptance Criteria:**  
  * Deteksi *Encoding* cerdas untuk memperbaiki teks berantakan pada karakter Kanji, Hangul, atau Cyrillic.  
  * Mendukung tata letak *Right-to-Left* (RTL) secara otomatis untuk bahasa Arab/Ibrani.  
* **Edge Case & Mitigasi:** Sematkan *fallback font* ringan jika ponsel lama pengguna tidak memiliki *font* bawaan untuk bahasa tertentu.

## **FASE 7: Detail Sepele yang Menjadi Pain Point Utama**

**Konteks Bisnis:** Menyelesaikan hal-hal yang sering dianggap wajar oleh pengembang namun dibenci pengguna luring.

* **Logika "Previous":** Menekan "Sebelumnya" setelah lagu berjalan \> 5 detik harus mengulang lagu dari awal, bukan pindah ke lagu sebelumnya.  
* **Sleep Timer:** Gunakan efek *Fade-Out* selama 60 detik terakhir sebelum musik mati agar tidak mengagetkan pengguna yang setengah tertidur.  
* **Penghapusan Aman:** Jauhkan letak tombol "Hapus dari Playlist" dengan "Hapus File". Gunakan ikon berbeda dan dialog berwarna merah untuk penghapusan permanen.  
* **Kontrol Layar Kunci:** Beri ruang kosong di antara tombol *Play/Pause/Next* untuk menghindari layar tertekan tidak sengaja di dalam saku.

## **FASE 8: Interaksi Instan (Fitur "Like" & Auto-Playlist)**

**Konteks Bisnis:** Menyediakan jalan pintas emosional dengan memangkas proses pembuatan playlist menjadi satu klik.

* **User Story:** Sebagai pengguna, saya ingin menandai lagu dengan ikon hati agar otomatis masuk ke daftar putar khusus.  
* **Acceptance Criteria:**  
  * Ikon Hati tersedia di layar *Now Playing* dan daftar utama.  
  * Otomatis membuat playlist "Favorites" di latar belakang.  
  * Berikan *haptic feedback* dan animasi mikro saat ikon ditekan.  
* **Edge Case & Mitigasi:** Jika pengguna menghapus tanda suka (unlike) saat lagu sedang diputar dalam playlist *Favorites*, jangan langsung hilangkan lagu tersebut agar antrean tidak rusak. Biarkan lagu berada di sana (mungkin dibuat transparan) hingga daftar di-*refresh*.

## **FASE 9: Pengalaman Mendengarkan Tingkat Lanjut (Advanced Playback)**

**Konteks Bisnis:** Menyempurnakan fungsionalitas inti untuk pendengar musik intensif.

* **Gapless Playback:** Menghilangkan jeda diam 1-2 detik antar trek agar transisi lagu (terutama album *live*) benar-benar menyatu.  
* **Smart Shuffle:** Gunakan algoritma *pseudo-random* agar lagu dari artis/album yang sama didistribusikan merata dan tidak diputar berurutan.  
* **Lirik Lokal:** Deteksi otomatis metadata lirik dalam MP3 atau file berformat .lrc di folder yang sama untuk sinkronisasi lirik.  
* **Privasi Layar Kunci:** Sediakan sakelar opsional agar sampul album tidak mengambil alih layar kunci secara mencolok, atau gunakan efek *Gaussian Blur* yang tebal.