package com.tugasakhir.controller;

import com.tugasakhir.model.ContactInfo;
import com.tugasakhir.model.SystemLink;
import com.tugasakhir.repository.ContactInfoRepository;
import com.tugasakhir.repository.SystemLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/settings")
@RequiredArgsConstructor
public class SystemSettingsController {
    
    private final ContactInfoRepository contactInfoRepo;
    private final SystemLinkRepository systemLinkRepo;
    
    // ===== ADMIN ENDPOINTS (Require Auth) =====
    
    @GetMapping("/contact-info")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
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
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
    
    @PostMapping("/contact-info")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> updateContactInfo(@RequestBody ContactInfo contactInfo) {
        try {
            List<ContactInfo> existing = contactInfoRepo.findAll();
            ContactInfo toSave;
            
            if (!existing.isEmpty()) {
                toSave = existing.get(0);
                toSave.setAddress(contactInfo.getAddress());
                toSave.setPhone(contactInfo.getPhone());
                toSave.setEmail(contactInfo.getEmail());
                toSave.setOperatingHours(contactInfo.getOperatingHours());
            } else {
                toSave = contactInfo;
            }
            
            ContactInfo saved = contactInfoRepo.save(toSave);
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Contact info updated");
                put("data", saved);
            }});
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
    
    // ===== SYSTEM LINKS ENDPOINTS =====
    
    @GetMapping("/system-links")
    public ResponseEntity<?> getAllSystemLinks() {
        try {
            List<SystemLink> links = systemLinkRepo.findAll();
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("data", links);
                put("total", links.size());
            }});
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
    
    @GetMapping("/system-links/active")
    public ResponseEntity<?> getActiveSystemLinks() {
        try {
            List<SystemLink> links = systemLinkRepo.findByIsActiveTrue();
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("data", links);
            }});
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
    
    @GetMapping("/system-links/{id}")
    public ResponseEntity<?> getSystemLink(@PathVariable Integer id) {
        try {
            var link = systemLinkRepo.findById(id);
            if (link.isEmpty()) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Link not found");
                }});
            }
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("data", link.get());
            }});
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
    
    @PostMapping("/system-links")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> createSystemLink(@RequestBody SystemLink systemLink) {
        try {
            SystemLink saved = systemLinkRepo.save(systemLink);
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Link created");
                put("data", saved);
            }});
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
    
    @PutMapping("/system-links/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> updateSystemLink(@PathVariable Integer id, @RequestBody SystemLink systemLink) {
        try {
            var existing = systemLinkRepo.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Link not found");
                }});
            }
            
            SystemLink toUpdate = existing.get();
            toUpdate.setLinkName(systemLink.getLinkName());
            toUpdate.setLinkType(systemLink.getLinkType());
            toUpdate.setLinkUrl(systemLink.getLinkUrl());
            toUpdate.setDescription(systemLink.getDescription());
            toUpdate.setIsActive(systemLink.getIsActive());
            
            SystemLink saved = systemLinkRepo.save(toUpdate);
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Link updated");
                put("data", saved);
            }});
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
    
    @DeleteMapping("/system-links/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> deleteSystemLink(@PathVariable Integer id) {
        try {
            if (!systemLinkRepo.existsById(id)) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Link not found");
                }});
            }
            systemLinkRepo.deleteById(id);
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Link deleted");
            }});
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }
}
