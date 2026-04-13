# Header Template System - Setup Guide

## 📋 Overview
Sistem header yang dapat digunakan kembali untuk semua halaman CAMABA (Peserta Mendaftar).

## ✅ File yang telah dibuat

### 1. **`src/main/resources/static/components/header.html`**
   - Template HTML header dengan struktur lengkap
   - Logo + Nama UHN di kiri
   - Menu navigasi: Dashboard, Fakultas (dropdown), Alur, Biaya, Kontak
   - User dropdown dengan nama dan Logout
   - Responsive design (mobile + desktop)
   - Bootstrap 5.3.0 compatible
   - Bootstrap Icons (bi-*) icons

**Fitur:**
- ✅ Logo placeholder (custom styles, bisa ganti dengan logo UHN yang sesuai)
- ✅ Dropdown Fakultas dengan 10+ opsi
- ✅ Student name dari sessionStorage (userData.fullName)
- ✅ Auto active menu berdasarkan current page
- ✅ Kompatibel dengan existing logout() function

### 2. **`src/main/resources/static/components/header-loader.js`**
   - Script untuk load header component ke halaman
   - Auto-execute scripts dalam header HTML
   - Error handling jika fetch gagal

**Cara Kerja:**
1. Fetch `components/header.html`
2. Insert ke container `#headerContainer`
3. Execute semua scripts dalam header
4. Console log untuk debugging

### 3. **Updated Halaman CAMABA**

#### `src/main/resources/static/daftar-ulang.html`
- ✅ Tambah Bootstrap Icons CSS
- ✅ Replace navbar lama dengan `<div id="headerContainer"></div>`
- ✅ Add header-loader.js sebelum `</body>`
- ✅ Update margin-top: 76px untuk account header

#### `src/main/resources/static/ujian.html`
- ✅ Bootstrap Icons CSS sudah ada
- ✅ Replace navbar dengan header container
- ✅ Add header-loader.js
- ✅ Update margin-top: 20px

#### `src/main/resources/static/dashboard-camaba.html`
- ✅ Tambah Bootstrap Icons CSS
- ✅ Replace navbar dengan header container
- ✅ Add header-loader.js
- ✅ Update margin-top: 20px

## 🔧 Cara Menambah Header ke Halaman Lain

Untuk halaman CAMABA baru (selain ujian, daftar-ulang, dashboard):

### 1. **Add Header Container di `<body>`**
```html
<body>
    <!-- Header Container (akan di-load oleh header-loader.js) -->
    <div id="headerContainer"></div>
    
    <!-- Main Content -->
    <div class="your-main-container">
        <!-- Content here -->
    </div>
```

### 2. **Add Bootstrap Icons CSS (jika belum ada)**
Di dalam `<head>`:
```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
```

### 3. **Add Header Loader Script sebelum `</body>`**
```html
    <!-- Load Header Component -->
    <script src="/components/header-loader.js"></script>
</body>
```

### 4. **Update margin/padding jika menggunakan fixed header**
Jika halaman gunanya `fixed-top`, update main container dengan:
```css
margin-top: 76px;  /* atau sesuai kebutuhan */
```

## 📱 Fitur Header

### Menu Items:
1. **Dashboard** → Links ke `/dashboard-camaba.html`
2. **Fakultas** → Dropdown dengan link ke website fakultas (external)
3. **Alur Pendaftaran** → Scroll ke section footer (TBD)
4. **Biaya / UKT** → Download PDF (TBD - perlu setup)
5. **Kontak** → Scroll ke footer section

### User Dropdown:
- Nama Student (dari sessionStorage userData.fullName)
- Profil link (conditional - hidden by default, bisa diaktifkan)
- Logout button (menggunakan existing logout() function)

## 🎨 Styling

### Color Scheme:
- Primary: `#27ae60` (Hijau)
- Dark Primary: `#1a472a`
- Light Primary: `#52c77e`

### Responsive Breakpoints:
- Mobile: xs
- Tablet: md (768px cutoff untuk logo text)
- Desktop: lg+

### CSS Classes Available:
```css
.navbar-nav .nav-link:hover    /* Hover effect */
.navbar-nav .nav-link.active   /* Active state */
.dropdown-menu                  /* Dropdown styling */
.dropdown-item:hover            /* Item hover */
```

## ⚙️ Konfigurasi

### 1. **Logo**
Edit di `components/header.html`:
```html
<img src="/images/logo-uhn.png" alt="Logo UHN" ... />
```
Pastikan ada file `/images/logo-uhn.png` atau update path

### 2. **Student Name Display**
Header otomatis read dari:
```javascript
sessionStorage.getItem('userData')  // expected: {fullName: "Nama Student", ...}
```

Jika halaman tidak set userData, tampil "User" sebagai fallback

### 3. **Download Biaya/UKT**
Update di `components/header.html` line ~200:
```javascript
// TODO: Ganti dengan link PDF yang sesuai
// window.open('/docs/biaya-ukt.pdf', '_blank');
```

### 4. **Footer Scrolling**
Memerlukan footer section dengan ID:
```html
<section id="footerSection">
    <!-- Footer content -->
</section>
```

## 🔐 Security Notes

1. **Logout Function**: Menggunakan existing logout() jika ada
2. **Session Storage**: Data student dimuat dari sessionStorage
3. **External Links**: Links ke fakultas buka di tab baru (target="_blank")

## 📊 Browser Compatibility

✅ Chrome/Edge (latest)
✅ Firefox (latest)
✅ Safari (latest)
✅ Mobile browsers

Font Awesome Icons: `fas fa-*`
Bootstrap Icons: `bi bi-*`

## 🐛 Troubleshooting

### Header tidak muncul?
1. Check console untuk error messages
2. Pastikan header-loader.js di-load sebelum `</body>`
3. Pastikan `<div id="headerContainer"></div>` ada di body

### Icons tidak tampil?
1. Pastikan Bootstrap Icons CSS sudah di-load (check `<head>`)
2. Pastikan internet connection untuk CDN

### Student name tidak tampil?
1. Pastikan sessionStorage memiliki `userData` key
2. Format: `{fullName: "Nama Student", ...}`
3. Check browser console untuk debug: `sessionStorage.getItem('userData')`

### Logout tidak berfungsi?
1. Pastikan halaman punya `logout()` function
2. Jika tidak, fallback akan clear sessionStorage dan redirect ke index

## 📝 Next Steps

1. ✅ Test header loading di ketiga halaman CAMABA
2. ⏳ Update `Biaya/UKT` link ke PDF yang sesuai
3. ⏳ Setup footer section dengan ID `footerSection`
4. ⏳ Customize logo sesuai universal branding UHN
5. ⏳ Add header ke halaman CAMABA lain jika ada

---

**Last Updated**: April 2026
**System**: Spring Boot 3.3.0 + Bootstrap 5.3.0
**Status**: ✅ Ready for Testing
