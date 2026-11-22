package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.ChiNhanh;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChiNhanhRepository extends JpaRepository<ChiNhanh, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);}
