/**
 * COMMON AUTHENTICATION UTILITIES
 * Shared error handling for all pages
 * Include this in all HTML pages with: <script src="/common-auth.js"></script>
 */

const AUTH_TOKEN_KEY = 'authToken';

// ✅ Safe storage access with Tracking Prevention handling
function safeGetStorageItem(key, storageType = 'sessionStorage') {
    try {
        if (storageType === 'localStorage') {
            return localStorage.getItem(key);
        } else {
            return sessionStorage.getItem(key);
        }
    } catch (e) {
        console.warn(`⚠️ [Storage] Tracking Prevention: Cannot access ${storageType}:`, e.message);
        return null;
    }
}

function safeSetStorageItem(key, value, storageType = 'sessionStorage') {
    try {
        if (storageType === 'localStorage') {
            localStorage.setItem(key, value);
        } else {
            sessionStorage.setItem(key, value);
        }
        return true;
    } catch (e) {
        console.warn(`⚠️ [Storage] Tracking Prevention: Cannot access ${storageType}:`, e.message);
        return false;
    }
}

function safeRemoveStorageItem(key, storageType = 'sessionStorage') {
    try {
        if (storageType === 'localStorage') {
            localStorage.removeItem(key);
        } else {
            sessionStorage.removeItem(key);
        }
        return true;
    } catch (e) {
        console.warn(`⚠️ [Storage] Tracking Prevention: Cannot access ${storageType}:`, e.message);
        return false;
    }
}

// ✅ Check sessionStorage first (per-tab), fallback to localStorage (backward compat)
let authToken = safeGetStorageItem(AUTH_TOKEN_KEY, 'sessionStorage') || safeGetStorageItem(AUTH_TOKEN_KEY, 'localStorage');
let loginRedirectListenerAdded = false;

/**
 * Show authentication error modal with custom message
 */
function showAuthRequiredModal(customMessage = null) {
    const authModal = document.getElementById('authRequiredModal');
    if (authModal) {
        // Update message if provided
        if (customMessage) {
            const messageElement = authModal.querySelector('.text-muted');
            if (messageElement) {
                messageElement.innerHTML = customMessage;
                console.error('🔴 Auth Error Modal: ' + customMessage);
            }
        }
        
        // ✅ AGGRESSIVE FIX: Force show modal with inline styles
        // Remove any display:none or conflicting styles
        authModal.style.cssText = `
            position: fixed !important;
            top: 0 !important;
            left: 0 !important;
            width: 100% !important;
            height: 100% !important;
            display: flex !important;
            z-index: 9999 !important;
            background: rgba(0, 0, 0, 0.7) !important;
            align-items: center !important;
            justify-content: center !important;
            padding: 0 !important;
            margin: 0 !important;
            overflow: hidden !important;
        `;
        
        // Make sure modal dialog is visible
        const modalDialog = authModal.querySelector('.modal-dialog');
        if (modalDialog) {
            modalDialog.style.cssText = `
                position: relative !important;
                display: flex !important;
                width: auto !important;
                margin: 0 auto !important;
                max-width: 420px !important;
                z-index: 10000 !important;
            `;
        }
        
        // Make sure modal content is visible
        const modalContent = authModal.querySelector('.modal-content');
        if (modalContent) {
            modalContent.style.cssText = `
                display: block !important;
                background: white !important;
                border-radius: 12px !important;
                box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3) !important;
                border: none !important;
                padding: 0 !important;
                margin: 0 !important;
                width: 100% !important;
            `;
        }
        
        // Make sure modal body is visible
        const modalBody = authModal.querySelector('.modal-body');
        if (modalBody) {
            modalBody.style.cssText = `
                display: block !important;
                padding: 40px 30px !important;
                text-align: center !important;
                background: white !important;
                border-radius: 12px !important;
            `;
        }
        
        // ✅ Ensure button is properly set up and visible
        if (!loginRedirectListenerAdded) {
            const loginBtn = document.getElementById('loginRedirectBtn');
            if (loginBtn) {
                // Force button visibility with aggressive inline styles
                loginBtn.style.cssText = `
                    display: block !important;
                    visibility: visible !important;
                    opacity: 1 !important;
                    pointer-events: auto !important;
                    background: #1a472a !important;
                    color: white !important;
                    border: none !important;
                    padding: 12px 30px !important;
                    font-weight: 600 !important;
                    border-radius: 8px !important;
                    cursor: pointer !important;
                    font-size: 1rem !important;
                    transition: all 0.3s ease !important;
                    width: 100% !important;
                    text-align: center !important;
                    margin-top: 15px !important;
                `;
                
                // Add click handler
                loginBtn.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    console.log('🔗 Redirecting to login...');
                    window.location.href = '/login.html';
                });
                
                loginRedirectListenerAdded = true;
                console.log('✅ Login button properly configured');
            } else {
                console.warn('⚠️ Login button not found');
            }
        }
        
        console.log('✅ Auth modal displayed with visibility forced');
    } else {
        // Fallback: If modal not found, use alert
        console.warn('⚠️ Auth modal element not found, using alert fallback');
        alert(customMessage || 'Authentication required');
    }
}

/**
 * Enhanced authentication check with detailed error handling
 * Returns true if authenticated, false if not
 */
async function checkAuthenticationWithErrorHandling(apiEndpoint = '/api/camaba/profile') {
    // ✅ Check sessionStorage first (per-tab), fallback to localStorage
    authToken = safeGetStorageItem(AUTH_TOKEN_KEY, 'sessionStorage') || safeGetStorageItem(AUTH_TOKEN_KEY, 'localStorage');
    
    if (!authToken) {
        console.warn('❌ No auth token stored - User needs to log in');
        showAuthRequiredModal('Anda belum login. Silahkan login untuk melanjutkan.');
        return false;
    }

    try {
        console.log('🔍 Checking authentication with token...');
        const response = await fetch(apiEndpoint, {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + authToken,
                'Content-Type': 'application/json'
            }
        });

        console.log('Response status: ' + response.status + ' ' + response.statusText);

        if (response.status === 401) {
            console.error('❌ Token unauthorized (401) - Token invalid atau expired');
            showAuthRequiredModal('Sesi Anda telah berakhir. Silahkan login kembali.');
            safeRemoveStorageItem(AUTH_TOKEN_KEY, 'localStorage');
            safeRemoveStorageItem(AUTH_TOKEN_KEY, 'sessionStorage');
            return false;
        }
        
        if (response.status === 403) {
            console.error('❌ Token forbidden (403) - User tidak memiliki akses');
            showAuthRequiredModal('Anda tidak memiliki akses untuk halaman ini.');
            return false;
        }

        if (response.status === 500) {
            try {
                const errorData = await response.json();
                const errorMsg = errorData.message || 'Server error - User tidak ditemukan di database';
                console.error('❌ Server error (500): ' + errorMsg);
                showAuthRequiredModal('❌ Server Error: ' + errorMsg + '<br><br>Silahkan logout dan login kembali.');
            } catch (e) {
                showAuthRequiredModal('❌ Server Error: Koneksi ke server gagal.<br><br>Silahkan coba lagi nanti.');
            }
            return false;
        }

        if (!response.ok) {
            try {
                const errorData = await response.json();
                const errorMsg = errorData.message || ('HTTP ' + response.status + ' error');
                console.error('❌ Auth check failed (' + response.status + '): ' + errorMsg);
                showAuthRequiredModal('❌ Authentication Error: ' + errorMsg + '<br><br>Silahkan login kembali.');
            } catch (e) {
                showAuthRequiredModal('❌ Authentication Error (HTTP ' + response.status + ')<br><br>Silahkan login kembali.');
            }
            return false;
        }

        const userData = await response.json();
        console.log('✓ Authentication verified - User: ' + (userData.email || userData.fullName || 'User'));
        return true;
    } catch (error) {
        console.error('❌ Network error during auth check: ' + error.message);
        showAuthRequiredModal('❌ Jaringan Error: ' + error.message + '<br><br>Periksa koneksi internet Anda.');
        return false;
    }
}

/**
 * Generic API call with automatic error handling
 * Returns response if successful, null if failed
 */
async function fetchWithAuthErrorHandling(url, options = {}) {
    authToken = safeGetStorageItem(AUTH_TOKEN_KEY, 'localStorage') || safeGetStorageItem(AUTH_TOKEN_KEY, 'sessionStorage');
    
    if (!authToken && !options.allowNoAuth) {
        console.warn('❌ No auth token for API call to: ' + url);
        showAuthRequiredModal('Anda belum login. Silahkan login untuk melanjutkan.');
        return null;
    }

    const defaultHeaders = {
        'Content-Type': 'application/json',
        ...(authToken && { 'Authorization': 'Bearer ' + authToken })
    };

    const fetchOptions = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers
        }
    };

    try {
        const response = await fetch(url, fetchOptions);

        if (response.status === 401 || response.status === 403) {
            showAuthRequiredModal('Sesi Anda telah berakhir. Silahkan login kembali.');
            safeRemoveStorageItem(AUTH_TOKEN_KEY, 'localStorage');
            safeRemoveStorageItem(AUTH_TOKEN_KEY, 'sessionStorage');
            return null;
        }

        if (response.status === 500) {
            try {
                const errorData = await response.json();
                const errorMsg = errorData.message || 'Server error';
                console.error('❌ API Error (500): ' + errorMsg);
                showAuthRequiredModal('❌ Server Error: ' + errorMsg + '<br><br>Silahkan coba lagi nanti.');
            } catch (e) {
                showAuthRequiredModal('❌ Server Error: Koneksi ke server gagal.');
            }
            return null;
        }

        if (!response.ok) {
            try {
                const errorData = await response.json();
                console.error('❌ API Error (' + response.status + '): ' + errorData.message);
            } catch (e) {
                console.error('❌ API Error (' + response.status + '): ' + response.statusText);
            }
            return null;
        }

        return response;
    } catch (error) {
        console.error('❌ Network error: ' + error.message);
        showAuthRequiredModal('❌ Jaringan Error: ' + error.message + '<br><br>Periksa koneksi internet Anda.');
        return null;
    }
}

/**
 * Robust logout function - clears ALL auth data
 */
function logout() {
    console.log('🚪 Logout initiated...');
    
    // Clear ALL auth-related data
    try {
        localStorage.removeItem('authToken');
        localStorage.removeItem(AUTH_TOKEN_KEY);
        localStorage.removeItem('userRole');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('token');
    } catch (e) {
        console.warn('⚠️ Could not clear localStorage:', e.message);
    }
    
    // Also clear sessionStorage
    try {
        sessionStorage.clear();
    } catch (e) {
        console.warn('⚠️ Could not clear sessionStorage:', e.message);
    }
    
    console.log('✅ All auth data cleared');
    
    // Redirect to login with replace() for clean history
    console.log('🔄 Redirecting to login...');
    window.location.replace('/login.html');
}

console.log('✓ common-auth.js loaded');
