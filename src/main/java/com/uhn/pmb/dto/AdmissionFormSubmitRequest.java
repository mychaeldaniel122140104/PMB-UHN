package com.uhn.pmb.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO untuk menerima submission data dari form pendaftaran
 * Semua field dari form harus di-map ke sini dengan benar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionFormSubmitRequest {
    
    // ===== SELECTION TYPE & TESTING =====
    private Long selectionTypeId;
    private Long jenisSeleksiId;
    private Boolean requireTesting;
    
    // ===== PILIHAN PROGRAM STUDI =====
    private String programChoice1;
    private String programChoice2;
    private String programChoice3;
    
    // ===== DATA PRIBADI =====
    private String fullName;
    private String nik;
    private String addressMedan;
    private String residenceInfo;
    private String subdistrict;
    private String district;
    private String city;
    private String province;
    private String phoneNumber;
    private String email;
    private String birthPlace;
    private String birthDate;
    private String gender;
    private String religion;
    private String informationSource;
    
    // ===== DATA ORANG TUA - AYAH =====
    private String fatherNik;
    private String fatherName;
    private String fatherBirthDate;
    private String fatherEducation;
    private String fatherOccupation;
    private String fatherIncome;
    private String fatherPhone;
    private String fatherStatus;
    
    // ===== DATA ORANG TUA - IBU =====
    private String motherNik;
    private String motherName;
    private String motherBirthDate;
    private String motherEducation;
    private String motherOccupation;
    private String motherIncome;
    private String motherPhone;
    private String motherStatus;
    
    // ===== ALAMAT ORANG TUA =====
    private String parentSubdistrict;
    private String parentCity;
    private String parentProvince;
    private String parentPhone;
    
    // ===== DATA ASAL SEKOLAH =====
    private String schoolOrigin;
    private String schoolMajor;
    private Integer schoolYear;
    private String nisn;
    private String schoolCity;
    private String schoolProvince;
    
    // ===== FILE UPLOADS =====
    private MultipartFile photoId;
    private MultipartFile certificate;
    private MultipartFile transcript;
    
    // ✅ NEW: Optional file uploads for RANKING_NO_TEST wave
    private MultipartFile nilaiFile;
    private MultipartFile rankingFile;
    
    // ===== EXAM INFO (jika ada) =====
    private String examLocation;
    private String faculty;
    private String examRoom;
}
