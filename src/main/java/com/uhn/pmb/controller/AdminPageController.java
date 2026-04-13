package com.uhn.pmb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ✅ Simple page routing controller - NO authentication checks
 * 
 * Purpose: Provide convenient routes that redirect to actual HTML files
 * Security: Frontend JS handles auth check via common-auth.js
 * 
 * Routes:
 * - /admin/dashboard-validasi → /dashboard-admin-validasi.html
 * - /admin/dashboard-pusat → /dashboard-admin-pusat.html
 * - /admin/reenroll → /admin-reenroll.html
 */
@Controller
@RequestMapping("/admin")
public class AdminPageController {

    /**
     * Redirect /admin/dashboard-validasi → /dashboard-admin-validasi.html
     * Frontend JS will check auth on page load
     */
    @GetMapping("/dashboard-validasi")
    public String adminValidasiDashboard() {
        return "redirect:/dashboard-admin-validasi.html";
    }

    /**
     * Redirect /admin/dashboard-pusat → /dashboard-admin-pusat.html
     * Frontend JS will check auth on page load
     */
    @GetMapping("/dashboard-pusat")
    public String adminPusatDashboard() {
        return "redirect:/dashboard-admin-pusat.html";
    }

    /**
     * Redirect /admin/reenroll → /admin-reenroll.html
     * Frontend JS will check auth on page load
     */
    @GetMapping("/reenroll")
    public String adminReenroll() {
        return "redirect:/admin-reenroll.html";
    }

}
