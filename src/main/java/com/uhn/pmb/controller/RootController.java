package com.uhn.pmb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller untuk menangani root path dan redirect ke halaman utama
 */
@Controller
@RequestMapping("/")
public class RootController {

    /**
     * Redirect root path (/) ke index.html
     * Memastikan semua akses pertama kali masuk ke index.html
     */
    @GetMapping("")
    public String redirectToIndex() {
        return "redirect:/index.html";
    }

    /**
     * ✅ NEW: Redirect /dashboard-camaba → /dashboard-camaba.html
     * Menangani case ketika browser/code request tanpa .html extension
     */
    @GetMapping("dashboard-camaba")
    public String redirectDashboardCamaba() {
        return "redirect:/dashboard-camaba.html";
    }
}
