package com.tugasakhir.repository;

import com.tugasakhir.model.SystemLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface SystemLinkRepository extends JpaRepository<SystemLink, Integer> {
    Optional<SystemLink> findByLinkName(String linkName);
    List<SystemLink> findByIsActiveTrue();
    List<SystemLink> findByLinkType(String linkType);
}
