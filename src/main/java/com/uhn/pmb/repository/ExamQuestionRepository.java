package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ExamQuestion;
import com.uhn.pmb.entity.QuestionCategory;
import com.uhn.pmb.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.category = ?1 AND eq.approvalStatus = 'APPROVED' ORDER BY eq.createdAt DESC")
    List<ExamQuestion> findByCategoryAndApproved(QuestionCategory category);

    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.approvalStatus = ?1 ORDER BY eq.createdAt DESC")
    List<ExamQuestion> findByApprovalStatus(ApprovalStatus status);

    @Query("SELECT COUNT(eq) FROM ExamQuestion eq WHERE eq.approvalStatus = 'PENDING'")
    Long countPendingQuestions();

    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.approvalStatus = 'PENDING' ORDER BY eq.createdAt DESC")
    List<ExamQuestion> findAllPending();

    @Query("SELECT eq FROM ExamQuestion eq WHERE eq.category = ?1 AND eq.approvalStatus = 'APPROVED'")
    List<ExamQuestion> findByCategoryApproved(QuestionCategory category);
}
