package com.uhn.pmb.service;

import com.uhn.pmb.entity.*;
import com.uhn.pmb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudentRegistrationService {

    private final StudentRepository studentRepository;
    private final AdmissionFormRepository admissionFormRepository;
    private final VirtualAccountRepository virtualAccountRepository;
    private final SelectionTypeRepository selectionTypeRepository;
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final BrivaService brivaService;
    private final EmailService emailService;

    /**
     * Create student profile
     */
    public Student createStudentProfile(User user, String fullName, String nik) {
        Student student = Student.builder()
                .user(user)
                .fullName(fullName)
                .nik(nik)
                .build();
        
        return studentRepository.save(student);
    }

    /**
     * Register for admission
     */
    public AdmissionForm registerForAdmission(Student student, RegistrationPeriod period, 
                                             SelectionType selectionType, String programStudi) {
        AdmissionForm form = AdmissionForm.builder()
                .student(student)
                .period(period)
                .selectionTypeId(selectionType.getId())  // Store SelectionType ID instead of entity
                .formType(selectionType.getFormType())  // Simpan formType langsung
                .programStudi1(programStudi)
                .status(AdmissionForm.FormStatus.DRAFT)
                .build();
        
        return admissionFormRepository.save(form);
    }

    /**
     * Buy form and create virtual account
     */
    public VirtualAccount buyFormAndCreateVA(AdmissionForm form) throws Exception {
        // Get price from SelectionType using the stored ID
        SelectionType selectionType = selectionTypeRepository.findById(form.getSelectionTypeId())
                .orElseThrow(() -> new RuntimeException("SelectionType not found"));
        BigDecimal price = selectionType.getPrice();
        
        VirtualAccount va = VirtualAccount.builder()
                .student(form.getStudent())
                .admissionForm(form)
                .amount(price)
                .paymentType(VirtualAccount.PaymentType.REGISTRATION_FORM)
                .status(VirtualAccount.VAStatus.ACTIVE)
                .build();
        
        String vaNumber = brivaService.generateVirtualAccount(va);
        va.setVaNumber(vaNumber);
        va = virtualAccountRepository.save(va);
        
        log.info("Form purchased and VA created for student: {}", form.getStudent().getId());
        
        return va;
    }

    /**
     * Submit admission form
     */
    public void submitAdmissionForm(AdmissionForm form) {
        form.setStatus(AdmissionForm.FormStatus.SUBMITTED);
        form.setSubmittedAt(LocalDateTime.now());
        admissionFormRepository.save(form);
        
        log.info("Admission form submitted for student: {}", form.getStudent().getId());
    }

    /**
     * Create exam record for student
     */
    public Exam createExamRecord(Student student, RegistrationPeriod period) {
        String examNumber = generateExamNumber();
        
        Exam exam = Exam.builder()
                .student(student)
                .period(period)
                .examNumber(examNumber)
                .status(Exam.ExamStatus.PENDING)
                .build();
        
        return examRepository.save(exam);
    }

    /**
     * Mark exam as completed
     */
    public void markExamCompleted(Exam exam, Double score) {
        exam.setStatus(Exam.ExamStatus.COMPLETED);
        exam.setCompletedAt(LocalDateTime.now());
        examRepository.save(exam);
        
        // Create exam result
        ExamResult result = ExamResult.builder()
                .exam(exam)
                .student(exam.getStudent())
                .score(score)
                .status(ExamResult.ResultStatus.PENDING)
                .build();
        
        // Determine pass/fail (passing grade: 60)
        if (score >= 60) {
            result.setStatus(ExamResult.ResultStatus.PASSED);
            result.setAdmissionNumber(generateAdmissionNumber());
            result.setAdmissionPassword(exam.getStudent().getBirthDate().toString());
        } else {
            result.setStatus(ExamResult.ResultStatus.FAILED);
        }
        
        examResultRepository.save(result);
        log.info("Exam completed for student: {} with score: {}", exam.getStudent().getId(), score);
    }

    /**
     * Publish exam results
     */
    public void publishExamResults(RegistrationPeriod period) {
        List<Exam> exams = examRepository.findByPeriod_Id(period.getId());
        
        for (Exam exam : exams) {
            ExamResult result = examResultRepository.findByExam_Id(exam.getId()).orElse(null);
            if (result != null && result.getStatus() != ExamResult.ResultStatus.PUBLISHED) {
                result.setStatus(ExamResult.ResultStatus.PUBLISHED);
                result.setPublishedAt(LocalDateTime.now());
                examResultRepository.save(result);
                
                // Send notification email
                if (result.getStatus() == ExamResult.ResultStatus.PASSED) {
                    emailService.sendResultNotification(
                            exam.getStudent().getUser().getEmail(),
                            true,
                            result.getAdmissionNumber(),
                            result.getAdmissionPassword()
                    );
                } else {
                    emailService.sendResultNotification(
                            exam.getStudent().getUser().getEmail(),
                            false,
                            null,
                            null
                    );
                }
            }
        }
    }

    /**
     * Generate unique exam number
     */
    private String generateExamNumber() {
        String examNumber;
        do {
            long timestamp = System.currentTimeMillis() % 100000;
            long random = (long) (Math.random() * 100000);
            examNumber = String.format("UJI%05d%05d", timestamp, random);
        } while (examRepository.findByExamNumber(examNumber).isPresent());
        
        return examNumber;
    }

    /**
     * Generate unique admission number
     */
    private String generateAdmissionNumber() {
        return String.format("PMB%d%06d", 
                LocalDateTime.now().getYear(),
                (long) (Math.random() * 1000000));
    }

    /**
     * Get student's admission forms
     */
    public List<AdmissionForm> getStudentAdmissionForms(Student student) {
        return admissionFormRepository.findByStudent_Id(student.getId());
    }

    /**
     * Get student's exam
     */
    public Exam getStudentExam(Student student) {
        return examRepository.findByStudent_Id(student.getId()).orElse(null);
    }

    /**
     * Get exam result
     */
    public ExamResult getExamResult(Exam exam) {
        return examResultRepository.findByExam_Id(exam.getId()).orElse(null);
    }
}
