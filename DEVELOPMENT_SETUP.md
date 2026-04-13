# 🔥 Development Setup - Spring Boot Auto-Reload Guide

## Masalah Clasik: Perubahan HTML/JS tidak terlihat

Ini terjadi karena Spring Boot default serve dari `target/classes/static/` bukan dari `src/main/resources/static/`

---

## ✅ Solusi yang Sudah Diterapkan

### 1. **application.properties sudah dikonfigurasi**
```properties
# Serve static files langsung dari src (tanpa rebuild)
spring.web.resources.static-locations=file:src/main/resources/static/

# Disable semua cache
spring.web.resources.cache.period=0
spring.web.resources.cache.cachecontrol.max-age=0
spring.web.resources.cache.cachecontrol.no-cache=true
spring.web.resources.cache.cachecontrol.no-store=true
spring.web.resources.cache.cachecontrol.must-revalidate=true
spring.web.resources.chain.cache=false

# Thymeleaf - Disable cache
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=file:src/main/resources/templates/

# DevTools - Enable auto-reload
spring.devtools.restart.enabled=true
spring.devtools.livereload.enabled=true
```

### 2. **pom.xml sudah include spring-boot-devtools**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

---

## 🚀 Cara Menjalankan di Development Mode

### ✅ CARA YANG BENAR (Rekomendasi)

```bash
# 1. Stop server jika masih running

# 2. Build project dengan devtools
mvn clean install

# 3. RUN server dengan spring-boot:run (BUKAN java -jar)
mvn spring-boot:run
```

**Result:**
- ✅ Edit HTML/JS → Langsung keliatan
- ✅ Refresh browser → Perubahan muncul
- ✅ Tidak perlu rebuild

---

## ⚡ Fitur yang Aktif

| Fitur | Status |
|-------|--------|
| Live Reload Static Files | ✅ Aktif |
| Cache Disabled | ✅ Aktif |
| DevTools Restart | ✅ Aktif |
| Direct src/ Serving | ✅ Aktif |
| Browser Auto-Refresh | ✅ Bisa (dengan livereload extension) |

---

## 🔴 JANGAN LAKUKAN INI

❌ **Jangan pakai:**
```bash
java -jar target/pmb-system-1.0.0.jar --server.port=9500
```
👉 Ini akan membuat file ter-"freeze" dan tidak ada auto-reload

---

## 🆘 Troubleshooting

### Masalah: Perubahan masih tidak terlihat

**Solusi 1: Clear Browser Cache**
```
Tekan: Ctrl + Shift + R
atau
F12 → Network tab → Centang "Disable cache"
```

**Solusi 2: Server restart belum**
- Stop console (Ctrl + C)
- Maven akan auto-compile dan restart
- Tunggu sampai `Application started` keluar

**Solusi 3: File sync di IDE**
- File → Save All (Ctrl + S)
- Rebuild Project di IDE

### Masalah: "Spring Boot DevTools tidak bekerja"

Cek di console output:
```
.... Restarting application
.... Reloading static files ...
```

Kalau tidak ada, berarti:
1. DevTools dependency tidak terinstall → `mvn clean install`
2. Tidak pakai `mvn spring-boot:run` → Ganti ke command itu

---

## 📁 Folder Structure yang Benar

```
src/
 └── main/
     └── resources/
         ├── static/              ← Edit di sini (langsung keliatan)
         │   ├── dashboard-camaba.html
         │   ├── dashboard-admin-validasi.html
         │   ├── index.html
         │   └── ...
         ├── templates/           ← Thymeleaf templates
         └── application.properties  ← Config already updated ✅

target/
 └── classes/
     └── static/              ← Auto-copied by DevTools
```

---

## 🎯 Development Workflow yang Benar

```
1. Run: mvn spring-boot:run
   ↓
2. Edit: src/main/resources/static/dashboard-camaba.html
   ↓
3. Save file (Ctrl + S)
   ↓
4. Browser auto-refresh ATAU Ctrl + Shift + R
   ↓
5. Perubahan terlihat ✅
   ↓
6. Repeat dari step 2 (jangan rebuild, jangan restart)
```

---

## ✨ Pro Tips

1. **Gunakan Live Reload Extension:**
   - Chrome: "Live Reload" extension
   - Firefox: "LiveReload" extension
   → Halaman auto-refresh otomatis setiap kali save

2. **IDE Configuration:**
   - IntelliJ IDEA: Settings → Build → Compiler → Check "Build project automatically"
   - VS Code: Install "Spring Boot Extension Pack"

3. **Cepat testing:**
   - Jadi cuma perlu: Edit → Save → Refresh Browser
   - Tidak perlu rebuild = jauh lebih cepat ⚡

---

## 📋 Checklist Setup

- [x] application.properties sudah dikonfigurasi ✅
- [x] pom.xml include spring-boot-devtools ✅
- [ ] Jalankan `mvn clean install` ← Lakukan ini sekarang
- [ ] Jalankan `mvn spring-boot:run` ← Ganti command run kamu
- [ ] Edit file HTML/JS di src/
- [ ] Refresh browser → Perubahan muncul
- [ ] ✨ Selesai!

---

## 🚀 Siap untuk Production?

Setelah development selesai:

```bash
# Build JAR untuk production
mvn clean package

# DevTools akan di-exclude auto dari JAR
# Production build = murni, tanpa development tools
java -jar target/pmb-system-1.0.0.jar
```

---

**Happy Coding! 🎉**
