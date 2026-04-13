# 🔧 OpenAI/Gemini API Configuration Guide

Panduan lengkap untuk mengkonfigurasi OpenAI/Gemini API Key agar fitur "Generate Soal dengan AI" berfungsi dengan baik.

## 📋 Daftar Isi
1. [Prasyarat](#prasyarat)
2. [Cara Mendapatkan API Key](#cara-mendapatkan-api-key)
3. [Konfigurasi di Aplikasi](#konfigurasi-di-aplikasi)
4. [Testing dan Troubleshooting](#testing-dan-troubleshooting)
5. [Best Practices](#best-practices)

---

## 🔑 Prasyarat

- Akun Google/Gmail
- Akses ke Google Cloud Console
- Administrator akses ke aplikasi PMB

---

## 📖 Cara Mendapatkan API Key

### Step 1: Buat Google Cloud Project

1. Buka [Google Cloud Console](https://console.cloud.google.com/)
2. Klik **"Select a Project"** → **"New Project"**
3. Masukkan nama project (contoh: `PMB-Soal-Generator`)
4. Klik **Create**

### Step 2: Enable Gemini API

1. Di Cloud Console, cari **"Generative Language API"**
2. Klik hasil pencarian pertama
3. Klik **Enable** (jika belum aktif)
4. Tunggu beberapa detik hingga API diaktifkan

### Step 3: Buat API Key

1. Klik menu **"Credentials"** di sidebar kiri
2. Klik **"Create Credentials"** → **"API Key"**
3. Popup akan menampilkan API Key baru
4. Klik **"Copy"** untuk menyalin API Key
5. Simpan API Key di tempat yang aman

> ⚠️ **PENTING**: Jangan share API Key dengan siapa pun!
> Selalu gunakan environment variables, bukan hardcode di kode.

---

## 🔗 Konfigurasi di Aplikasi

### Option 1: Environment Variable (Recommended untuk Production)

#### Windows:

```batch
# Command Prompt
setx GEMINI_API_KEY "your-api-key-here"

# atau PowerShell
$env:GEMINI_API_KEY = "your-api-key-here"
```

Setelah set, restart aplikasi:
```batch
java -jar pmb-system-1.0.0.jar
```

#### Linux/Mac:

```bash
export GEMINI_API_KEY="your-api-key-here"
java -jar pmb-system-1.0.0.jar
```

### Option 2: File application.properties (Development Only)

1. Buka file: `src/main/resources/application.properties`
2. Cari baris: `gemini.api.key=${GEMINI_API_KEY:}`
3. Ubah menjadi:
   ```properties
   gemini.api.key=your-api-key-here
   ```
4. Restart aplikasi

> ⚠️ **SECURITY WARNING**: 
> Jangan commit file berisi API Key ke Git!
> Gunakan `.gitignore` untuk file berisi secrets.

### Option 3: Pass sebagai Java Argument

```bash
java -DGEMINI_API_KEY="your-api-key-here" -jar pmb-system-1.0.0.jar
```

---

## 🧪 Testing dan Troubleshooting

### Test API Key Configuration

1. Login sebagai Admin Pusat
2. Navigasi ke **Dashboard Admin** → **Admin Validasi**
3. Scroll ke section **"Generate Soal Baru dengan AI"**
4. Cek alert info - jika berisi:
   - ✅ **Informasi Penting**: API Key sudah dikonfigurasi dengan benar
   - ❌ **OpenAI API Key belum dikonfigurasi**: Ikuti step konfigurasi kembali

### Test Generate Soal

1. Di section "Generate Soal", isi form:
   - **Kategori**: Pilih salah satu
   - **Mata Pelajaran**: Contoh: Biologi
   - **Tingkat Kesulitan**: Pilih salah satu
   - **Jumlah Soal**: Min 1, Max 20
2. Klik **"Generate Soal dengan AI"**

#### Possible Responses:

| Response | Masalah | Solusi |
|----------|---------|--------|
| ✅ Soal berhasil di-generate | Tidak ada | Sukses! Cek di "Soal Menunggu Review" |
| ❌ OpenAI API Key belum dikonfigurasi | API Key tidak ditemukan | Set environment variable atau update application.properties |
| ❌ Quota OpenAI API telah habis | Kredit Google Cloud habis | Top-up kredit di [Google Cloud Billing](https://console.cloud.google.com/billing) |
| ❌ Rate limit OpenAI API tercapai | Terlalu banyak request | Tunggu 1-2 menit sebelum retry |
| ❌ Error dari OpenAI API | API Key invalid/expired | Verify API Key di Google Cloud Console |

### Troubleshooting Checklist

- ✅ API Key sudah dikopi dengan benar (tanpa spasi ekstra)?
- ✅ Environment variable sudah di-set dan aplikasi sudah di-restart?
- ✅ Generative Language API sudah di-enable di Cloud Console?
- ✅ API Key belum pernah direset/revoked?
- ✅ Kredit Google Cloud masih cukup (minimal $1)?
- ✅ Firewall/Proxy tidak memblokir API request ke Google?

---

## 💡 Best Practices

### Security

1. **Jangan hardcode API Key**
   - Selalu gunakan environment variables
   - Jangan commit file dengan API Key ke repository

2. **Rotate API Key secara berkala**
   - Ganti API Key setiap 3-6 bulan
   - Delete old keys dari Google Cloud Console

3. **Monitor API Usage**
   - Cek billing di [Google Cloud Billing](https://console.cloud.google.com/billing)
   - Set budget alerts untuk mencegah cost yang tidak terduga

### Performance

1. **Limit jumlah soal yang di-generate sekaligus**
   - Max 20 per request (sudah ter-limit di sistem)
   - Untuk 100+ soal, generate dalam batch beberapa kali

2. **Cache hasil generate**
   - Sistem sudah menyimpan soal ke database
   - Reuse soal yang sudah di-approve

3. **Error Handling**
   - User akan mendapat pesan error jelas jika ada masalah
   - Cek logs aplikasi: `logs/application.log`

### Maintenance

1. **Regular testing**
   - Test fitur generate setiap minggu
   - Monitor usage patterns

2. **Update monitoring**
   - Set up alert jika API request gagal
   - Monitor warning messages di dashboard

3. **Documentation**
   - Update dokumen ini jika ada perubahan
   - Share dengan admin lain di tim

---

## 📞 Support

Jika ada masalah:

1. Cek logs aplikasi: `sudo tail -f logs/application.log` (Linux) atau buka file logs (Windows)
2. Verify API Key di [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
3. Check quota dan billing di [Google Cloud Billing](https://console.cloud.google.com/billing)
4. Contact: Support Team atau Cloud Administrator

---

## 📚 Referensi

- [Google Generative AI Documentation](https://ai.google.dev/tutorials/rest_quickstart)
- [Google Cloud Pricing](https://cloud.google.com/generative-ai/pricing)
- [Troubleshooting Guide](https://ai.google.dev/tutorials/rest_quickstart#troubleshooting)

---

**Last Updated**: December 2024
**Version**: 1.0
