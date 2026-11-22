package com.cnw.medical_system.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Entity
@Table(name = "lesion_episode")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LesionEpisode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "episode_id")
    Long id;
    @Column(name = "patient_id", nullable = false)
    Long patientId;
    @Column(name = "start_date", nullable = false)
    LocalDate startDate;
    @Column(name = "end_date")
    LocalDate endDate;
    @Column(name = "description", length = 1000)
    String description;
}
