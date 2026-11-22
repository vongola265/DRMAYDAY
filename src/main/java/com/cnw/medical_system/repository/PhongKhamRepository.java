package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.PhongKham;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PhongKhamRepository extends JpaRepository<PhongKham, Long> {
    List<PhongKham> findByBranchId(Long branchId);
    boolean existsByBranchId(Long branchId);
    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);
}
