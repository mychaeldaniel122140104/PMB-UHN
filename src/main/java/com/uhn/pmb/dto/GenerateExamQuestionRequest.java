package com.uhn.pmb.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateExamQuestionRequest {
    private String category; // IPA, IPS, Psikotes, Bahasa
    private String subject; // Biologi, Kimia, Fisika, Matematika, etc
    private String difficulty; // Easy, Medium, Hard
    private String topic; // Topik yang ingin di-generate (optional, untuk AI context)
    private Integer count; // Jumlah soal yang ingin di-generate (default 1)
}
