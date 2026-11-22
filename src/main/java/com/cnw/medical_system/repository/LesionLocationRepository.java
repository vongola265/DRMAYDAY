package com.cnw.medical_system.repository;

import com.cnw.medical_system.entity.LesionLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LesionLocationRepository extends JpaRepository<LesionLocation, Long> {
    List<LesionLocation> findByEpisode_IdOrderByCreatedAtAsc(Long episodeId);
    List<LesionLocation> findByEpisode_Id(Long episodeId);
}
