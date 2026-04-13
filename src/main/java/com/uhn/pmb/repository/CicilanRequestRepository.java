package com.uhn.pmb.repository;

import com.uhn.pmb.entity.CicilanRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CicilanRequestRepository extends JpaRepository<CicilanRequest, Long> {

    // Find by student
    @Query("SELECT cr FROM CicilanRequest cr WHERE cr.student.id = :studentId ORDER BY cr.createdAt DESC")
    List<CicilanRequest> findByStudentId(@Param("studentId") Long studentId);

    Optional<CicilanRequest> findByAdmissionFormId(Long admissionFormId);

    // Find by status
    @Query("SELECT cr FROM CicilanRequest cr WHERE cr.status = :status ORDER BY cr.createdAt DESC")
    Page<CicilanRequest> findByStatus(@Param("status") CicilanRequest.CicilanRequestStatus status, Pageable pageable);

    // Find pending requests
    @Query("SELECT cr FROM CicilanRequest cr WHERE cr.status = 'PENDING' ORDER BY cr.createdAt DESC")
    Page<CicilanRequest> findPendingRequests(Pageable pageable);

    // Find by program studi
    @Query("SELECT cr FROM CicilanRequest cr WHERE cr.programStudi.id = :programStudiId AND cr.status = :status")
    List<CicilanRequest> findByProgramStudiAndStatus(@Param("programStudiId") Long programStudiId, @Param("status") CicilanRequest.CicilanRequestStatus status);

    // Find all for a student with status
    @Query("SELECT cr FROM CicilanRequest cr WHERE cr.student.id = :studentId AND cr.status = :status")
    List<CicilanRequest> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") CicilanRequest.CicilanRequestStatus status);
}
