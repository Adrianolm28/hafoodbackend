package com.hafood.sistema.repository;

import com.hafood.sistema.domain.pos.Comanda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {
}