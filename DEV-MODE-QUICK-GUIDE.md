# Quick Reference: Development Setup

## Jawaban Singkat ke Pertanyaan Kamu

### "Harus rebuild setiap kali edit HTML/JS?"

**SEBELUM (Masalah):**
```
Edit file → Rebuild → Restart → Coba → Repeat
❌ SANGAT LAMBAT
```

**SESUDAH (Solusi):**
```
Edit file → Save → Refresh browser
✅ LANGSUNG KELIHAT
```

---

## Setup yang Sudah Done ✅

### 1. application.properties
- ✅ Static location: `file:src/main/resources/static/`
- ✅ Cache: disabled
- ✅ DevTools: enabled

### 2. pom.xml
- ✅ spring-boot-devtools: added

### 3. Scripts
- ✅ `DEV-MODE-START.bat` (Windows)
- ✅ `dev-mode-start.sh` (Linux/Mac)

---

## Cara Mulai (Copy-Paste)

### Windows:
```bash
Double-click: DEV-MODE-START.bat
```

### Linux/Mac:
```bash
chmod +x dev-mode-start.sh
./dev-mode-start.sh
```

---

## Development Workflow

| Step | Action | Result |
|------|--------|--------|
| 1 | Run script (1 kali saja) | Server starts ✅ |
| 2 | Edit: `src/main/resources/static/file.html` | Changes saved |
| 3 | Ctrl+S (save di text editor) | Auto-compile |
| 4 | Ctrl+Shift+R (browser) | Changes visible ✅ |
| Repeat | From step 2 | No rebuild needed! |

---

## Yang Berbeda Sekarang

### SEBELUM ❌
```
Edit HTML in src/
  → Not visible (serves dari target/)
  → Need mvn clean install
  → Need server restart
  → Takes 30-60 seconds
```

### SESUDAH ✅
```
Edit HTML in src/
  → DevTools direct serve
  → Auto-compile
  → Instant visible
  → Takes 1-2 seconds
```

---

## Production Build (Ketika Deploy)

```bash
# Build JAR untuk production (DevTools otomatis di-exclude)
mvn clean package

# Run di production
java -jar target/pmb-system-1.0.0.jar
```

---

## Troubleshooting Quick Fixes

| Problem | Fix |
|---------|-----|
| Changes not visible | Ctrl+Shift+R (hard refresh) |
| DevTools not working | Make sure using `mvn spring-boot:run` NOT `java -jar` |
| Still seeing old version | Check: F12 → Network → Disable cache |
| Server not starting | Check console for errors, sometimes port 9500 busy |

---

## Key Points

- 🚀 **Jangan pakai:** `java -jar target/app.jar`
- 🚀 **Pakai:** `mvn spring-boot:run`
- 📝 **Edit di:** `src/main/resources/static/`
- 🔄 **Refresh:** Browser (Ctrl+Shift+R)
- ⚡ **Tidak perlu rebuild atau restart manual**

---

**That's it! Sekarang development seharusnya jauh lebih cepat 🎉**
