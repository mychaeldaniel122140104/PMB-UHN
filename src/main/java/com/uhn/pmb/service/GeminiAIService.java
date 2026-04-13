package com.uhn.pmb.service;

import com.uhn.pmb.dto.GenerateExamQuestionRequest;
import com.uhn.pmb.entity.ApprovalStatus;
import com.uhn.pmb.entity.ExamQuestion;
import com.uhn.pmb.entity.QuestionCategory;
import com.uhn.pmb.entity.QuestionDifficulty;
import com.uhn.pmb.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class GeminiAIService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Generate multiple exam questions using Google Gemini AI via REST API
     */
    public List<ExamQuestion> generateMultipleExamQuestions(GenerateExamQuestionRequest request, User createdBy) {
        int count = request.getCount() != null ? request.getCount() : 1;
        if (count > 20) count = 20;
        if (count < 1) count = 1;

        List<ExamQuestion> questions = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            try {
                questions.add(generateExamQuestion(request, createdBy));
            } catch (Exception e) {
                log.error("Error generating question {}/{}: {}", i + 1, count, e.getMessage());
                // Re-throw to stop batch processing on first error with real API
                throw new RuntimeException("Gagal generate soal ke-" + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return questions;
    }

    /**
     * Generate exam questions using Google Gemini AI via REST API
     */
    public ExamQuestion generateExamQuestion(GenerateExamQuestionRequest request, User createdBy) {
        try {
            // Detailed API key logging
            log.info("🔐 === GEMINI API KEY CHECK ===");
            log.info("🔐 API Key is null? {}", geminiApiKey == null);
            log.info("🔐 API Key is empty? {}", geminiApiKey != null && geminiApiKey.isEmpty());
            log.info("🔐 API Key length: {}", geminiApiKey != null ? geminiApiKey.length() : "NULL");
            log.info("🔐 API Key first 10 chars: {}", geminiApiKey != null && geminiApiKey.length() >= 10 ? geminiApiKey.substring(0, 10) + "..." : "TOO_SHORT");
            
            if (!isApiKeyConfigured()) {
                log.error("❌ Gemini API key NOT configured properly!");
                log.error("❌ Check application.properties - gemini.api.key must be set");
                throw new RuntimeException("GEMINI_API_KEY tidak dikonfigurasi. Pastikan ada di application.properties: gemini.api.key=YOUR_KEY");
            }

            log.info("✅ API Key is valid");
            log.info("🚀 Calling Gemini API for: {} - {} ({})", request.getCategory(), request.getSubject(), request.getDifficulty());
            
            String requestBody = buildGeminiRequest(request);
            String response = callGeminiAPI(requestBody);

            log.info("✓ Gemini API response received successfully, parsing...");
            return parseAIResponse(response, request, createdBy);
        } catch (Exception e) {
            log.error("❌ Error calling Gemini API: {}", e.getMessage());
            log.error("❌ Exception Type: {}", e.getClass().getName());
            log.error("❌ Full Stack Trace:", e);
            
            // Throw with more detail
            throw new RuntimeException("Gemini API Error: " + e.getMessage(), e);
        }
    }

    /**
     * Build request body untuk Gemini API
     */
    private String buildGeminiRequest(GenerateExamQuestionRequest request) throws Exception {
        String prompt = buildPrompt(request);

        Map<String, Object> requestMap = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = List.of(
                Map.of("text", prompt)
        );

        content.put("parts", parts);
        requestMap.put("contents", List.of(content));

        return objectMapper.writeValueAsString(requestMap);
    }

    /**
     * Call Gemini API
     */
    private String callGeminiAPI(String requestBody) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        String url = GEMINI_API_URL + "?key=" + geminiApiKey;
        log.info("🌐 Calling Gemini API URL: {}", GEMINI_API_URL);
        log.info("📤 Request body: {}", requestBody);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            log.info("📥 Gemini Response Status: {}", response.getStatusCode());
            log.info("📥 Gemini Response Body: {}", response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorBody = response.getBody();
                log.error("❌ Gemini API returned non-2xx status: {} - Body: {}", response.getStatusCode(), errorBody);
                throw new RuntimeException("Gemini API returned error: " + response.getStatusCode() + " - " + errorBody);
            }

            return response.getBody();
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("❌ Gemini API Client Error (4xx): {} - Response: {}", e.getStatusCode(), errorBody);
            throw new RuntimeException("Gemini API Client Error (" + e.getStatusCode() + "): " + errorBody, e);
        } catch (HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("❌ Gemini API Server Error (5xx): {} - Response: {}", e.getStatusCode(), errorBody);
            throw new RuntimeException("Gemini API Server Error (" + e.getStatusCode() + "): " + errorBody, e);
        } catch (RestClientException e) {
            log.error("❌ Gemini API Communication Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to communicate with Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Build prompt untuk Gemini AI
     */
    private String buildPrompt(GenerateExamQuestionRequest request) {
        String category = request.getCategory() != null ? request.getCategory() : "IPA";
        String subject = request.getSubject() != null ? request.getSubject() : "Biologi";
        String difficulty = request.getDifficulty() != null ? request.getDifficulty() : "MEDIUM";
        String topic = request.getTopic() != null ? request.getTopic() : "umum";

        return String.format("""
                Buatkan 1 soal ujian pilihan ganda untuk kategori %s, mata pelajaran %s, level kesulitan %s.
                Topik: %s
                
                Soal harus:
                - Berdasarkan standar ujian nasional SMA
                - Pilihan ganda dengan 5 opsi (A, B, C, D, E)
                - Memiliki penjelasan singkat untuk jawaban yang benar
                - Realistis dan edukatif
                
                Berikan respons dalam format JSON HANYA (tanpa markdown):
                {
                    "questionText": "...",
                    "optionA": "...",
                    "optionB": "...",
                    "optionC": "...",
                    "optionD": "...",
                    "optionE": "...",
                    "correctAnswer": "A",
                    "explanation": "..."
                }
                """, category, subject, difficulty, topic);
    }

    /**
     * Parse Gemini API response
     */
    private String extractTextFromGeminiResponse(String response) throws Exception {
        log.debug("🔍 Parsing Gemini response...");
        JsonNode root = objectMapper.readTree(response);
        
        // Log response structure untuk debugging
        log.debug("Response structure keys: {}", root.fieldNames().hasNext() ? "Has fields" : "Empty");
        
        if (root.has("error")) {
            JsonNode error = root.get("error");
            String errorMsg = error.has("message") ? error.get("message").asText() : error.toString();
            log.error("🔴 Gemini API returned error: {}", errorMsg);
            throw new RuntimeException("Gemini API Error: " + errorMsg);
        }
        
        if (root.has("candidates") && root.get("candidates").isArray()) {
            JsonNode firstCandidate = root.get("candidates").get(0);
            if (firstCandidate.has("content") && firstCandidate.get("content").has("parts")) {
                JsonNode parts = firstCandidate.get("content").get("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).get("text").asText();
                    log.debug("✅ Successfully extracted text from Gemini response");
                    return text;
                }
            }
        }
        
        log.error("🔴 Unexpected Gemini API response format. Response: {}", response);
        throw new RuntimeException("Unexpected Gemini API response format. Response not in expected structure: " + response.substring(0, Math.min(500, response.length())));
    }

    /**
     * Parse AI response dan create ExamQuestion object
     */
    private ExamQuestion parseAIResponse(String response, GenerateExamQuestionRequest request, User createdBy) {
        try {
            // Extract text content from Gemini response
            String textContent = extractTextFromGeminiResponse(response);
            String jsonString = extractJsonFromResponse(textContent);
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            ExamQuestion question = ExamQuestion.builder()
                    .category(QuestionCategory.valueOf(request.getCategory().toUpperCase()))
                    .subject(request.getSubject())
                    .difficulty(QuestionDifficulty.valueOf(request.getDifficulty().toUpperCase()))
                    .questionText(jsonNode.get("questionText").asText())
                    .optionA(jsonNode.get("optionA").asText())
                    .optionB(jsonNode.get("optionB").asText())
                    .optionC(jsonNode.get("optionC").asText())
                    .optionD(jsonNode.get("optionD").asText())
                    .optionE(jsonNode.has("optionE") ? jsonNode.get("optionE").asText() : "")
                    .correctAnswer(jsonNode.get("correctAnswer").asText())
                    .explanation(jsonNode.get("explanation").asText())
                    .approvalStatus(ApprovalStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .createdBy(createdBy)
                    .build();

            log.info("Successfully generated question: {}", question.getQuestionText().substring(0, 50));
            return question;
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage());
        }
    }

    /**
     * Extract JSON dari response
     */
    private String extractJsonFromResponse(String response) {
        int jsonStart = response.indexOf('{');
        int jsonEnd = response.lastIndexOf('}');

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            String json = response.substring(jsonStart, jsonEnd + 1);
            log.debug("✅ Successfully extracted JSON from response");
            return json;
        }

        log.error("🔴 No valid JSON found in Gemini response");
        log.error("🔴 Response content: {}", response.substring(0, Math.min(1000, response.length())));
        throw new RuntimeException("No valid JSON found in AI response. Response: " + response.substring(0, Math.min(500, response.length())));
    }

    /**
     * Generate mock question sebagai fallback
     */
    private ExamQuestion generateMockQuestion(GenerateExamQuestionRequest request, User createdBy) {
        String subject = request.getSubject() != null ? request.getSubject() : "";
        String category = request.getCategory() != null ? request.getCategory() : "";
        int randomId = (int)(System.currentTimeMillis() % 100);
        
        // Generate varied mock questions berdasarkan subject
        String questionText;
        String optA, optB, optC, optD, optE;
        String correctAns;
        String explanation;
        
        if (subject.toLowerCase().contains("ekonomi")) {
            String[] questions = {
                "Sistem ekonomi campuran menggabungkan mekanisme pasar dengan...",
                "Dalam ekonomi makro, inflasi didefinisikan sebagai...",
                "Fungsi utama bank sentral adalah mengatur...",
                "Pertumbuhan ekonomi diukur dari peningkatan...",
                "Kebijakan moneter ketat diterapkan ketika..."
            };
            String[] opts = {
                "A|intervensi pemerintah|mekanisme murni pasar|regulasi bilateral|kontrol penuh|kebijakan luar",
                "B|penurunan harga umum|peningkatan harga umum|stabil sekali|penurunan daya beli|pertumbuhan GDP",
                "C|jumlah uang beredar|nilai tukar|suku bunga|pajak|subsidi pemerintah",
                "D|pertumbuhan PDB|pertumbuhan per capita|pertumbuhan sektor|pertumbuhan ekspor|pertumbuhan investasi",
                "E|inflasi rendah|deflasi tinggi|pertumbuhan melambat|pengangguran naik|konsumsi menurun"
            };
            String[] answers = {"B", "B", "A", "A", "A"};
            String[] exps = {
                "Ekonomi campuran menggabungkan kekuatan pasar dengan intervensi pemerintah",
                "Inflasi adalah peningkatan harga-harga secara umum dan terus-menerus",
                "Bank sentral mengatur jumlah uang beredar dan stabilitas ekonomi",
                "PDB (Produk Domestik Bruto) adalah ukuran utama pertumbuhan ekonomi",
                "Kebijakan moneter ketat dilakukan untuk mengendalikan inflasi"
            };
            
            int idx = randomId % questions.length;
            questionText = questions[idx];
            String[] optParts = opts[idx].split("\\|");
            optA = optParts[1];
            optB = optParts[2];
            optC = optParts[3];
            optD = optParts[4];
            optE = optParts[5];
            correctAns = answers[idx];
            explanation = exps[idx];
        } else if (subject.toLowerCase().contains("sejarah")) {
            questionText = "Peristiwa penting dalam sejarah Indonesia adalah...";
            optA = "Perang Diponegoro melawan Belanda (1825-1830)";
            optB = "Pertempuran Surabaya melawan Jepang";
            optC = "Proklamasi Kemerdekaan 2 September 1945";
            optD = "Konferensi Meja Bundar tahun 1949";
            optE = "G30S/PKI tahun 1965";
            correctAns = "C";
            explanation = "Proklamasi Kemerdekaan 2 September 1945 adalah momentum penting pengakuan kemerdekaan Indonesia";
        } else {
            questionText = "[MOCK - " + subject + "] Berapa hasil dari 2 + 2?";
            optA = "3";
            optB = "4";
            optC = "5";
            optD = "6";
            optE = "7";
            correctAns = "B";
            explanation = "Jawaban yang benar adalah 4 karena 2 + 2 = 4";
        }
        
        return ExamQuestion.builder()
                .category(QuestionCategory.valueOf(request.getCategory().toUpperCase()))
                .subject(request.getSubject())
                .difficulty(QuestionDifficulty.valueOf(request.getDifficulty().toUpperCase()))
                .questionText(questionText)
                .optionA(optA)
                .optionB(optB)
                .optionC(optC)
                .optionD(optD)
                .optionE(optE)
                .correctAnswer(correctAns)
                .explanation(explanation)
                .approvalStatus(ApprovalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
    }

    public boolean isApiKeyConfigured() {
        return geminiApiKey != null && !geminiApiKey.isEmpty() && !geminiApiKey.isBlank();
    }
}
