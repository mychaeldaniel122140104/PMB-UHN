package com.uhn.pmb.dto;

import com.uhn.pmb.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileRequest {
    private String fullName;
    private String nik;
    private LocalDate birthDate;
    private String birthPlace;
    private Student.Gender gender;
    private String address;
    private String phoneNumber;
    private String parentName;
    private String parentPhone;
    private String schoolOrigin;
    private String schoolYear;
}
