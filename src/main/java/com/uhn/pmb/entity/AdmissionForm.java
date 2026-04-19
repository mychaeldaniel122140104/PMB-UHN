package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "admission_forms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "period_id")
    @JsonIgnore
    private RegistrationPeriod period;

    // ✅ NEW: Store JenisSeleksi (program type) ID directly
    @Column(name = "jenis_seleksi_id", nullable = true)
    private Long jenisSeleksiId;

    // ✅ Store SELECTION_TYPE_ID as Long value (NOT entity relasi)
    @Column(name = "selection_type_id", nullable = true)
    private Long selectionTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "form_type", nullable = false)
    private SelectionType.FormType formType;

    // ===== PILIHAN PROGRAM STUDI =====
    @Column(name = "program_studi_1")
    private String programStudi1;

    @Column(name = "program_studi_2")
    private String programStudi2;

    @Column(name = "program_studi_3")
    private String programStudi3;

    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    // ===== DATA PRIBADI =====
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "nik")
    private String nik;

    @Column(name = "address_medan")
    private String addressMedan;

    @Column(name = "residence_info")
    private String residenceInfo;

    @Column(name = "subdistrict")
    private String subdistrict;

    @Column(name = "district")
    private String district;

    @Column(name = "city")
    private String city;

    @Column(name = "province")
    private String province;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(name = "gender")
    private String gender;

    @Column(name = "religion")
    private String religion;

    @Column(name = "information_source")
    private String informationSource;

    // ===== DATA ORANG TUA - AYAH (FATHER) =====
    @Column(name = "father_nik")
    private String fatherNik;

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "father_birth_date")
    private String fatherBirthDate;

    @Column(name = "father_education")
    private String fatherEducation;

    @Column(name = "father_occupation")
    private String fatherOccupation;

    @Column(name = "father_income")
    private String fatherIncome;

    @Column(name = "father_phone")
    private String fatherPhone;

    @Column(name = "father_status")
    private String fatherStatus;

    // ===== DATA ORANG TUA - IBU (MOTHER) =====
    @Column(name = "mother_nik")
    private String motherNik;

    @Column(name = "mother_name")
    private String motherName;

    @Column(name = "mother_birth_date")
    private String motherBirthDate;

    @Column(name = "mother_education")
    private String motherEducation;

    @Column(name = "mother_occupation")
    private String motherOccupation;

    @Column(name = "mother_income")
    private String motherIncome;

    @Column(name = "mother_phone")
    private String motherPhone;

    @Column(name = "mother_status")
    private String motherStatus;

    // ===== ALAMAT ORANG TUA =====
    @Column(name = "parent_subdistrict")
    private String parentSubdistrict;

    @Column(name = "parent_city")
    private String parentCity;

    @Column(name = "parent_province")
    private String parentProvince;

    @Column(name = "parent_phone")
    private String parentPhone;

    // ===== DATA ASAL SEKOLAH =====
    @Column(name = "school_origin")
    private String schoolOrigin;

    @Column(name = "school_major")
    private String schoolMajor;

    @Column(name = "school_year")
    private Integer schoolYear;

    @Column(name = "nisn")
    private String nisn;

    @Column(name = "school_city")
    private String schoolCity;

    @Column(name = "school_province")
    private String schoolProvince;

    // ===== DOKUMEN PENDUKUNG =====
    @Column(name = "photo_id_path")
    private String photoIdPath;

    @Column(name = "certificate_path")
    private String certificatePath;

    @Column(name = "transcript_path")
    private String transcriptPath;

    // ✅ NEW: Optional file paths for RANKING_NO_TEST wave
    @Column(name = "nilai_file_path")
    private String nilaiFilePath;

    @Column(name = "ranking_file_path")
    private String rankingFilePath;

    // ===== PAYMENT METHOD (HYBRID SYSTEM) =====
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod = PaymentMethod.SIMULATION;

    // ===== STATUS & TIMESTAMPS =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormStatus status = FormStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum FormStatus {
        DRAFT,
        SUBMITTED,
        VERIFIED,
        REJECTED,
        WAITING_PAYMENT
    }

    public enum PaymentMethod {
        SIMULATION("Virtual Account - Mode Simulasi"),
        MANUAL("Transfer Manual - Upload Bukti")
        ;
        
        private final String label;
        PaymentMethod(String label) {
            this.label = label;
        }
        public String getLabel() { return label; }
    }
}
