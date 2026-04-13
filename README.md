# PMB HKBP Nommensen - Sistem Penerimaan Mahasiswa Baru

Sistem terintegrasi berbasis Spring Boot untuk mengelola proses penerimaan mahasiswa baru (PMB) di Universitas HKBP Nommensen dengan fitur lengkap mulai dari pendaftaran, ujian online, integrasi pembayaran BRIVA, hingga daftar ulang.

## 📋 Daftar Isi

- [Fitur Utama](#fitur-utama)
- [Persyaratan Sistem](#persyaratan-sistem)
- [Instalasi & Setup](#instalasi--setup)
- [Konfigurasi](#konfigurasi)
- [Struktur Proyek](#struktur-proyek)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Integrasi Eksternal](#integrasi-eksternal)
- [Panduan Pengguna](#panduan-pengguna)
- [Troubleshooting](#troubleshooting)

## ✨ Fitur Utama

### Untuk Calon Mahasiswa (CAMABA)
- ✅ Registrasi akun dan login
- ✅ Lengkapi profil data diri
- ✅ Pilih program studi dan jalur masuk
- ✅ Pembayaran formulir melalui Virtual Account (BRIVA)
- ✅ Pengisian formulir pendaftaran
- ✅ Generate kartu ujian otomatis (PDF dengan QR Code)
- ✅ Akses ujian online (integrasi Google Form)
- ✅ Lihat hasil ujian dan status kelulusan
- ✅ Daftar ulang dengan pembayaran cicilan
- ✅ Kirim pertanyaan ke admin

### Untuk Admin Validasi
- ✅ Validasi data calon mahasiswa
- ✅ Verifikasi berkas prestasi/ranking
- ✅ Generate Virtual Account manual untuk pembayaran
- ✅ Melihat data peserta ujian
- ✅ Validasi data daftar ulang
- ✅ Ekspor data dalam format CSV/XLSX
- ✅ Komunikasi langsung dengan calon mahasiswa
- ✅ Menjawab pertanyaan dari calon mahasiswa

### Untuk Admin Pusat (PSI)
- ✅ Kelola periode pendaftaran (gelombang)
- ✅ Atur jenis seleksi dan harga formulir
- ✅ Kelola informasi landing page
- ✅ Pengaturan field isian formulir (dinamis)
- ✅ Atur jadwal publikasi kelulusan
- ✅ Kelola tarif cicilan per program studi
- ✅ Kelola akun admin validasi
- ✅ Pratinjau tampilan sistem dari perspektif user lain

## 🛠️ Persyaratan Sistem

### Software
- **Java**: JDK 21 atau lebih tinggi
- **Maven**: 3.8.0 atau lebih tinggi
- **MySQL**: 8.0 atau lebih tinggi (atau PostgreSQL 13+)
- **Git**: untuk version control
- **Browser**: Chrome, Firefox, Edge, Safari (terbaru)

### Hardware Minimal
- CPU: 2 cores
- RAM: 4 GB
- Disk Space: 10 GB

### Koneksi Internet
- Untuk SMTP Gmail
- Untuk integrasi BRIVA API
- Untuk Google Form callback

## 🚀 Instalasi & Setup

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/pmb-hkbp.git
cd pmb-hkbp
```

### 2. Setup Database
```sql
-- MySQL
CREATE DATABASE pmb_uhn CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pmb_uhn;

-- PostgreSQL
CREATE DATABASE pmb_uhn;
```

### 3. Configure Environment Variables
Buat file `.env` di root project atau set environment variables:

```properties
# Database
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=pmb_uhn
MYSQL_USER=root
MYSQL_PASSWORD=

# Email (Gmail SMTP)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password

# JWT
JWT_SECRET=your-super-secret-key-change-this-in-production

# BRIVA Payment API
BRIVA_API_KEY=your-briva-api-key
BRIVA_API_SECRET=your-briva-api-secret
BRIVA_CLIENT_ID=your-briva-client-id
```

### 4. Build Project
```bash
mvn clean install
```

### 5. Run Application
```bash
mvn spring-boot:run
```

Aplikasi akan berjalan di `http://localhost:8080`

## ⚙️ Konfigurasi

### MySQL Connection (application.properties)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pmb_uhn?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=
```

### PostgreSQL Connection (application-postgresql.properties)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pmb_uhn
spring.datasource.username=postgres
spring.datasource.password=
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## 📁 Struktur Proyek

```
pmb-system/
├── src/
│   ├── main/
│   │   ├── java/com/uhn/pmb/
│   │   │   ├── config/              # Security & Configuration
│   │   │   ├── controller/          # REST API Controllers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── entity/              # JPA Entities
│   │   │   ├── repository/          # Data Access Layer
│   │   │   ├── service/             # Business Logic
│   │   │   ├── security/            # JWT & Security
│   │   │   └── PmbApplication.java  # Main Class
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   ├── templates/           # HTML Pages
│   │   │   └── static/              # CSS, JS, Images
│   └── test/                        # Unit & Integration Tests
├── pom.xml                          # Maven Dependencies
├── README.md                        # This file
└── .gitignore
```

## 🔌 API Endpoints

### Authentication
```
POST   /api/auth/register           - Register new account
POST   /api/auth/login              - Login
POST   /api/auth/validate-token     - Validate JWT token
GET    /api/auth/health             - Health check
```

### Student (CAMABA)
```
GET    /api/camaba/profile          - Get student profile
PUT    /api/camaba/profile          - Update student profile
GET    /api/camaba/registration-periods  - Get open periods
GET    /api/camaba/selection-types/{periodId}  - Get selection types
POST   /api/camaba/register-admission   - Register for admission
POST   /api/camaba/buy-form/{formId}    - Buy form & create VA
GET    /api/camaba/admission-status     - Get admission status
GET    /api/camaba/exam              - Get exam details
```

### Admin
```
POST   /api/admin/periods           - Create registration period
GET    /api/admin/periods           - Get all periods
POST   /api/admin/selection-types   - Create selection type
GET    /api/admin/forms-to-validate - Get forms for validation
PUT    /api/admin/forms/{formId}/validate  - Validate form
POST   /api/admin/publish-results/{periodId}  - Publish results
GET    /api/admin/reenrollments     - Get reenrollments
PUT    /api/admin/reenrollments/{id}/validate - Validate reenrollment
```

## 💾 Database Schema

### Core Tables
- **users** - User accounts dengan role-based access
- **students** - Data mahasiswa baru
- **registration_periods** - Periode/gelombang pendaftaran
- **selection_types** - Jenis seleksi per periode
- **admission_forms** - Formulir pendaftaran
- **virtual_accounts** - Virtual Account pembayaran (BRIVA)
- **exams** - Data ujian calon mahasiswa
- **exam_results** - Hasil ujian dan kelulusan
- **reenrollments** - Data daftar ulang
- **notifications** - Log notifikasi/email
- **system_configurations** - Konfigurasi sistem

## 🔗 Integrasi Eksternal

### BRIVA Payment Gateway
```
API Endpoint: https://apibriva.bri.co.id/v2
Fitur:
- Generate Virtual Account otomatis
- Verification pembayaran (callback)
- Inquiry status VA
- Cancel VA
```

### Gmail SMTP (Email Notifications)
```
SMTP Server: smtp.gmail.com
Port: 587
Fitur:
- Konfirmasi registrasi
- Notifikasi VA payment
- Reminder ujian
- Pengumuman hasil
- Notifikasi daftar ulang
```

### OpenAI/Gemini API (Generate Soal Otomatis)
```
API Endpoint: https://generativelanguage.googleapis.com/v1beta/models/gemini-pro
Fitur:
- Generate soal ujian otomatis dengan AI
- Mendukung berbagai kategori (IPA, IPS, Psikotes, Bahasa)
- Customizable tingkat kesulitan (Mudah, Sedang, Sulit)
- Batch generate hingga 20 soal sekaligus

⚠️ SETUP REQUIRED: Lihat OPENAI_API_SETUP.md untuk konfigurasi lengkap
```

### Google Forms (Ujian Online)
```
Integrasi:
- Redirect ke Google Form ujian
- Prefilled nomor ujian otomatis
- Pengambilan hasil ujian
- Scoring otomatis
```

## 📖 Panduan Pengguna

### Untuk Calon Mahasiswa
1. Buka `http://localhost:8080`
2. Klik "Daftar Sekarang"
3. Isi email dan password
4. Verifikasi email (jika diperlukan)
5. Login dan lengkapi data diri
6. Pilih program studi dan jalur masuk
7. Beli formulir dan dapatkan Virtual Account
8. Transfer sesuai VA number yang didirim via email
9. Ikuti ujian sesuai jadwal
10. Cek hasil dan lakukan daftar ulang jika lulus

### Untuk Admin Validasi
1. Login dengan akun admin validasi
2. Akses `/admin/forms-to-validate` untuk melihat formulir
3. Validasi setiap formulir berdasarkan kelengkapan data
4. Kirim pesan ke calon mahasiswa jika data perlu diperbaiki
5. Export data untuk kebutuhan administrasi

### Untuk Admin Pusat
1. Login dengan akun admin pusat
2. Kelola periode pendaftaran di `/admin/periods`
3. Set jenis seleksi dan harga formulir
4. Atur inform asi landing page
5. Publikasikan hasil ujian sesuai jadwal
6. Monitor KPI melalui dashboard

## 🔧 Troubleshooting

### Error: Connection refused to MySQL
```
Solusi:
1. Pastikan MySQL running: sudo service mysql start
2. Check connection string di application.properties
3. Verified username & password
```

### Error: Email not sending
```
Solusi:
1. Enable "Less secure app access" di Gmail settings
2. Gunakan "App-specific password" (recommended)
3. Check SMTP configuration di application.properties
4. Verify port 587 not blocked
```

### Error: BRIVA API not responding
```
Solusi:
1. Check internet connection
2. Verify API credentials
3. Check API endpoint URL
4. Implement retry logic
```

### Error: Database tables not created
```
Solusi:
1. Check hibernate.ddl-auto=update di application.properties
2. Manu create tables dengan SQL script
3. Verify JPA entity annotations
```

## 📝 License

MIT License - Copyright © 2025 HKBP Nommensen

## 👥 Tim Pengembang

- **Mychael Daniel N** - 122140104
- Program Studi Teknik Informatika
- Fakultas Teknologi dan Industri
- Institut Teknologi Sumatera Lampung Selatan

## 📞 Support

Untuk pertanyaan atau masalah teknis:
- Email: support@uhn.ac.id
- Portal: https://pmb.uhn.ac.id

## 📚 Referensi Teknologi

- [Spring Boot Official Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Guide](https://spring.io/guides/topicals/spring-security-architecture/)
- [JPA & Hibernate](https://hibernate.org/orm/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8949)
- [Bootstrap Framework](https://getbootstrap.com/)
- [MySQL Documentation](https://dev.mysql.com/doc/)

---

**Last Updated**: 2 Februari 2025
**Version**: 1.0.0
