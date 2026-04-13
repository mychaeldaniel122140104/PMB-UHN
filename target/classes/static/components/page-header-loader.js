/**
 * Page Header Loader Script
 * Include di halaman SETELAH <body> tag (setelah <div id="headerContainer">)
 */

async function loadPageHeader() {
  try {
    // Fetch page-header.html
    const response = await fetch('/components/page-header.html');
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const headerHTML = await response.text();
    
    // Cari atau buat container untuk page header (setelah main header)
    let headerContainer = document.getElementById('pageHeaderContainer');
    if (!headerContainer) {
      // Jika belum ada, buat container setelah #headerContainer
      const mainHeader = document.getElementById('headerContainer');
      if (mainHeader) {
        headerContainer = document.createElement('div');
        headerContainer.id = 'pageHeaderContainer';
        mainHeader.parentNode.insertBefore(headerContainer, mainHeader.nextSibling);
      }
    }
    
    if (!headerContainer) {
      console.warn('⚠️ pageHeaderContainer not found - creating at start of body');
      headerContainer = document.createElement('div');
      headerContainer.id = 'pageHeaderContainer';
      document.body.insertBefore(headerContainer, document.body.firstChild.nextSibling);
    }
    
    // Insert page header HTML
    headerContainer.innerHTML = headerHTML;
    
    // Execute scripts dalam page header HTML
    const scripts = headerContainer.querySelectorAll('script');
    scripts.forEach(oldScript => {
      const newScript = document.createElement('script');
      newScript.textContent = oldScript.innerHTML;
      oldScript.parentNode.replaceChild(newScript, oldScript);
    });
    
    console.log('✅ Page header component loaded successfully');
    
  } catch (error) {
    console.error('❌ Error loading page header:', error);
  }
}

// Load page header saat DOM ready atau langsung jika sudah ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', loadPageHeader);
} else {
  loadPageHeader();
}
