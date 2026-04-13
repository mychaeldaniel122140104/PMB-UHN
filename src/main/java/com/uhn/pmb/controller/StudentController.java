package com.uhn.pmb.controller;

import com.uhn.pmb.entity.Student;
import com.uhn.pmb.entity.User;
import com.uhn.pmb.repository.StudentRepository;
import com.uhn.pmb.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get all students - for admin dashboards to find incomplete registrations
     * Returns list of all students with basic info
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllStudents() {
        try {
            log.info("📋 Fetching all students...");
            
            List<Student> allStudents = studentRepository.findAll();
            
            List<Map<String, Object>> response = allStudents.stream().map(student -> {
                Map<String, Object> data = new HashMap<>();
                
                data.put("id", student.getId());
                data.put("fullName", student.getFullName());
                data.put("email", student.getUser() != null ? student.getUser().getEmail() : "");
                data.put("phoneNumber", student.getPhoneNumber());
                data.put("nik", student.getNik());
                data.put("gender", student.getGender());
                data.put("createdAt", student.getCreatedAt());
                
                return data;
            }).collect(Collectors.toList());
            
            log.info("✅ Returning {} students", response.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error fetching all students: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Get student profile by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudentById(@PathVariable Long id) {
        try {
            log.info("🔍 Fetching student with ID: {}", id);
            
            Student student = studentRepository.findById(id).orElse(null);
            if (student == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", student.getId());
            data.put("fullName", student.getFullName());
            data.put("email", student.getUser() != null ? student.getUser().getEmail() : "");
            data.put("phoneNumber", student.getPhoneNumber());
            data.put("nik", student.getNik());
            data.put("gender", student.getGender());
            data.put("birthDate", student.getBirthDate());
            data.put("birthPlace", student.getBirthPlace());
            
            log.info("✅ Student found: {}", student.getFullName());
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            log.error("❌ Error fetching student: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
