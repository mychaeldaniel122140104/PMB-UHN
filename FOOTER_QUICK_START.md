# Dynamic Footer - Quick Start Guide

## ⚡ 3-Minute Setup

### Step 1: Initialize Database (30 seconds)
1. Start your application (make sure H2 is running)
2. Open H2 Console: http://localhost:8080/h2-console
3. Copy-paste content from `FOOTER_SETTINGS_INIT.sql`
4. Click "Run" button
5. Verify success message appears

### Step 2: Test the API (30 seconds)
```bash
# Open browser and go to:
http://localhost:8080/admin/api/settings

# Expected response:
{
  "success": true,
  "data": {
    "email_support": "pmb@hkbpnommensen.ac.id",
    "whatsapp_admin": "628123456789",
    "bank_nama": "BRI (Bank Rakyat Indonesia)",
    "bank_rekening": "1234567890",
    "penerima_rekening": "HKBP Nommensen University",
    "facebook_url": "https://www.facebook.com/hkbpnommensen",
    "instagram_handle": "hkbp_nommensen",
    "website_url": "https://www.hkbpnommensen.ac.id"
  }
}
```

### Step 3: Verify on Frontend (1 minute)
1. Open in browser: http://localhost:8080/
2. Scroll to footer section
3. Verify footer displays:
   - ✅ HKBP Nommensen address
   - ✅ Email (pmb@hkbpnommensen.ac.id)
   - ✅ Phone number
   - ✅ WhatsApp link
   - ✅ Bank account details
   - ✅ Social media links (Facebook, Instagram)
   - ✅ Website link

## 🔄 Update Footer Data

### Via SQL (Easiest for testing)
```sql
UPDATE system_configurations 
SET config_value = 'new_email@example.com', updated_at = NOW()
WHERE config_key = 'email_support';
COMMIT;

-- Refresh page to see changes
```

### Via API (When Admin Panel is ready)
```bash
curl -X PUT \
  "http://localhost:8080/admin/api/settings/email_support" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"value": "new_email@example.com"}'
```

## 📝 Editable Settings

| Setting | Current Value | Purpose |
|---------|---------------|---------|
| `email_support` | pmb@hkbpnommensen.ac.id | Support email |
| `whatsapp_admin` | 628123456789 | WhatsApp contact |
| `bank_nama` | BRI (Bank...) | Bank name |
| `bank_rekening` | 1234567890 | Bank account |
| `penerima_rekening` | HKBP Nommensen University | Account owner |
| `facebook_url` | https://www.facebook.com/... | Facebook link |
| `instagram_handle` | hkbp_nommensen | Instagram handle |
| `website_url` | https://www.hkbpnommensen.ac.id | Website link |

## ✅ What Was Implemented

✓ **Dynamic Footer Loading** - Footer content loads from database
✓ **3 Entry Points** - Works on all landing pages
✓ **Auto-Refresh** - No cache issues
✓ **Error Handling** - Graceful fallback if API fails
✓ **Admin Editable** - Settings can be updated without code changes
✓ **Social Media Integration** - Direct links to social platforms
✓ **Contact Info** - Centralized contact management
✓ **Build Verified** - Maven build successful

## 📂 Files Reference

| File | Purpose |
|------|---------|
| `index.html` | Root landing page with dynamic footer |
| `index-pmb.html` | Alternative PMB landing page |
| `src/main/resources/static/index.html` | Static resources version |
| `FOOTER_SETTINGS_INIT.sql` | Database initialization script |
| `DYNAMIC_FOOTER_IMPLEMENTATION.md` | Full documentation |

## 🐛 Troubleshooting

### Footer Not Showing?
1. Check browser console (F12 → Console tab)
2. Verify API response: http://localhost:8080/admin/api/settings
3. Check database: `SELECT * FROM system_configurations;`
4. Clear browser cache (Ctrl+Shift+Delete)

### Settings Not Updating?
1. Verify SQL COMMIT was executed
2. Check isActive = true in database
3. Hard refresh page (Ctrl+F5)
4. Restart application

### Wrong Information Displayed?
1. Update database via SQL script
2. Refresh the footer by reloading page
3. Or manually edit in H2 Console

## 🚀 Next Steps

- [ ] Create Admin UI for managing footer settings
- [ ] Add admin panel at `/admin/settings`
- [ ] Implement role-based access control
- [ ] Add footer content preview
- [ ] Cache footer data for performance
- [ ] Add versioning/history for changes

## 📞 Support

For detailed documentation, see:
- `DYNAMIC_FOOTER_IMPLEMENTATION.md` - Full guide
- `FOOTER_SETTINGS_INIT.sql` - Database schema

Questions? Check the troubleshooting section above! 🎯
