package com.uhn.pmb.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for safely serializing AdmissionForm data
 * Breaks circular references and protects entity structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionFormDTO {
    private Long id;
    private Long studentId;
    
    // Registration Period Info
    private Long periodId;
    private String periodName;  // e.g., "Gelombang 1 - 2024"
    private String waveType;    // ✅ NEW: "REGULAR_TEST", "RANKING_NO_TEST", "EARLY_NO_TEST"
    
    // Selection Type Info
    private Long selectionTypeId;
    private String selectionTypeName;  // e.g., "Bebas Testing"
    private Long jenisSeleksiId;  // ✅ NEW: JenisSeleksi ID for program studi lookup
    private String formType;  // "MEDICAL" or "NON_MEDICAL" (Kedokteran/Non-Kedokteran)
    
    // ===== PILIHAN PROGRAM STUDI =====
    private String programStudi1;
    private String programStudi2;
    private String programStudi3;
    private String additionalInfo;
    
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
    
    // ===== DOKUMEN PENDUKUNG =====
    private String photoIdPath;
    private String certificatePath;
    private String transcriptPath;
    
    // Status & Timestamps
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Convert Entity to DTO to avoid Hibernate lazy-loading serialization issues
     */
    public static AdmissionFormDTO fromEntity(com.uhn.pmb.entity.AdmissionForm form) {
        if (form == null) return null;
        
        return AdmissionFormDTO.builder()
                .id(form.getId())
                .studentId(form.getStudent() != null ? form.getStudent().getId() : null)
                .selectionTypeId(form.getSelectionTypeId())
                .periodId(form.getPeriod() != null ? form.getPeriod().getId() : null)
                .formType(form.getFormType() != null ? form.getFormType().name() : null)
                .status(form.getStatus() != null ? form.getStatus().name() : "DRAFT")
                .createdAt(form.getCreatedAt())
                .submittedAt(form.getSubmittedAt())
                .updatedAt(form.getUpdatedAt())
                
                .fullName(form.getFullName())
                .nik(form.getNik())
                .birthDate(form.getBirthDate())
                .birthPlace(form.getBirthPlace())
                .gender(form.getGender())
                .phoneNumber(form.getPhoneNumber())
                .email(form.getEmail())
                .addressMedan(form.getAddressMedan())
                
                .subdistrict(form.getSubdistrict())
                .district(form.getDistrict())
                .city(form.getCity())
                .province(form.getProvince())
                .residenceInfo(form.getResidenceInfo())
                .religion(form.getReligion())
                .informationSource(form.getInformationSource())
                
                .fatherNik(form.getFatherNik())
                .fatherName(form.getFatherName())
                .fatherBirthDate(form.getFatherBirthDate())
                .fatherEducation(form.getFatherEducation())
                .fatherOccupation(form.getFatherOccupation())
                .fatherIncome(form.getFatherIncome())
                .fatherPhone(form.getFatherPhone())
                .fatherStatus(form.getFatherStatus())
                
                .motherNik(form.getMotherNik())
                .motherName(form.getMotherName())
                .motherBirthDate(form.getMotherBirthDate())
                .motherEducation(form.getMotherEducation())
                .motherOccupation(form.getMotherOccupation())
                .motherIncome(form.getMotherIncome())
                .motherPhone(form.getMotherPhone())
                .motherStatus(form.getMotherStatus())
                
                .parentSubdistrict(form.getParentSubdistrict())
                .parentCity(form.getParentCity())
                .parentProvince(form.getParentProvince())
                .parentPhone(form.getParentPhone())
                
                .schoolOrigin(form.getSchoolOrigin())
                .schoolMajor(form.getSchoolMajor())
                .schoolYear(form.getSchoolYear())
                .nisn(form.getNisn())
                .schoolCity(form.getSchoolCity())
                .schoolProvince(form.getSchoolProvince())
                
                .programStudi1(form.getProgramStudi1())
                .programStudi2(form.getProgramStudi2())
                .programStudi3(form.getProgramStudi3())
                
                .photoIdPath(form.getPhotoIdPath())
                .certificatePath(form.getCertificatePath())
                .transcriptPath(form.getTranscriptPath())
                
                .additionalInfo(form.getAdditionalInfo())
                .build();
    }
}
