/**
 * Footer Component Loader
 * Dynamically loads footer.html into pages with a footer container
 */

async function loadFooterComponent() {
    try {
        const response = await fetch('/components/footer.html');
        if (!response.ok) {
            console.error('❌ Failed to load footer component:', response.status);
            return;
        }

        const footerHTML = await response.text();
        
        // Find or create footer container
        let footerContainer = document.getElementById('footerContainer');
        if (!footerContainer) {
            footerContainer = document.createElement('div');
            footerContainer.id = 'footerContainer';
            document.body.appendChild(footerContainer);
        }
        
        // Insert footer HTML
        footerContainer.innerHTML = footerHTML;
        
        // Execute any scripts in the loaded HTML
        const scripts = footerContainer.querySelectorAll('script');
        scripts.forEach(script => {
            if (script.textContent) {
                try {
                    eval(script.textContent);
                } catch (e) {
                    console.error('Error executing footer script:', e);
                }
            }
        });
        
        console.log('✅ Footer component loaded successfully');
    } catch (error) {
        console.error('❌ Error loading footer component:', error);
    }
}

// Load footer when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadFooterComponent);
} else {
    loadFooterComponent();
}
