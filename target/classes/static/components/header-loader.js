/**
 * Header Loader Script
 * Digunakan untuk load header.html ke semua halaman CAMABA
 * Include ini di semua halaman CAMABA setelah <body>
 */

async function loadHeader() {
  try {
    // Fetch header.html
    const response = await fetch('/components/header.html');
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const headerHTML = await response.text();
    
    // Buat container untuk header jika belum ada
    let headerContainer = document.getElementById('headerContainer');
    if (!headerContainer) {
      headerContainer = document.createElement('div');
      headerContainer.id = 'headerContainer';
      // Insert sebelum elemen pertama di body
      document.body.insertBefore(headerContainer, document.body.firstChild);
    }
    
    // Insert header HTML
    headerContainer.innerHTML = headerHTML;
    
    // Execute scripts dalam header HTML
    const scripts = headerContainer.querySelectorAll('script');
    scripts.forEach(oldScript => {
      const newScript = document.createElement('script');
      newScript.textContent = oldScript.innerHTML;
      oldScript.parentNode.replaceChild(newScript, oldScript);
    });
    
    console.log('Header loaded successfully');
    
  } catch (error) {
    console.error('Error loading header:', error);
    // Optional: Show error message
    document.body.innerHTML = `<div style="padding: 20px; color: #d32f2f;">Error loading header. Please refresh the page.</div>` + document.body.innerHTML;
  }
}

// Load header saat DOM ready atau langsung jika sudah ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', loadHeader);
} else {
  loadHeader();
}
