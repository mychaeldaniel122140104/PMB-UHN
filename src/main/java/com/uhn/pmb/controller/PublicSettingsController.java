package com.uhn.pmb.controller;

import com.uhn.pmb.entity.ContactInfo;
import com.uhn.pmb.entity.SystemLink;
import com.uhn.pmb.repository.ContactInfoRepository;
import com.uhn.pmb.repository.SystemLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/public/settings")
@RequiredArgsConstructor
public class PublicSettingsController {
    
    private final ContactInfoRepository contactInfoRepo;
    private final SystemLinkRepository systemLinkRepo;
    
    // ===== PUBLIC ENDPOINTS (No Auth Required) =====
    
    @GetMapping("/contact-info")
    public ResponseEntity<?> getContactInfo() {
        try {
            List<ContactInfo> contacts = contactInfoRepo.findAll();
            if (contacts.isEmpty()) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("success", true);
                    put("message", "No contact info");
                    put("data", null);
                }});
            }
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("data", contacts.get(0));
            }});
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
    
    @GetMapping("/system-links")
    public ResponseEntity<?> getSystemLinksPublic() {
        try {
            List<SystemLink> links = systemLinkRepo.findByIsActiveTrue();
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("data", links);
                put("total", links.size());
            }});
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
    
    @GetMapping("/system-links/type/{type}")
    public ResponseEntity<?> getSystemLinksByType(@PathVariable String type) {
        try {
            List<SystemLink> links = systemLinkRepo.findByLinkType(type);
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("data", links);
                put("total", links.size());
            }});
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
}
