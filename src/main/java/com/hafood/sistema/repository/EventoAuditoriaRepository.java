package com.hafood.sistema.repository;

import com.hafood.sistema.domain.auditoria.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
}