package com.hafood.sistema.repository;


import com.hafood.sistema.domain.estructura.Sede;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SedeRepository extends JpaRepository<Sede, Long> {
}