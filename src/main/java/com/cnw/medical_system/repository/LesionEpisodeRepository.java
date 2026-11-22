package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.LesionEpisode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LesionEpisodeRepository extends JpaRepository<LesionEpisode, Long> {
    java.util.List<LesionEpisode> findByMedicalRecordId(Long medicalRecordId);
    java.util.List<LesionEpisode> findByPatientId(Long patientId);
}
