package com.mfa.project.repository;

import com.mfa.project.entity.QrToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QrTokenRepository extends JpaRepository<QrToken, Long> {

    Optional<QrToken> findByTokenAndActiveTrue(String token);
}