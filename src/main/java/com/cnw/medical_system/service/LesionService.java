package com.cnw.medical_system.service;

import com.cnw.medical_system.dto.request.LesionEpisodeRequest;
import com.cnw.medical_system.dto.request.LesionLocationRequest;
import com.cnw.medical_system.dto.response.LesionEpisodeResponse;
import com.cnw.medical_system.dto.response.LesionLocationResponse;
import com.cnw.medical_system.entity.LesionEpisode;
import com.cnw.medical_system.entity.LesionLocation;
import com.cnw.medical_system.repository.LesionEpisodeRepository;
import com.cnw.medical_system.repository.LesionLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LesionService {
    private final LesionEpisodeRepository episodes;
    private final LesionLocationRepository locations;
    private final MaydayPhotoService photos;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<LesionEpisodeResponse> listEpisodes() {
        return episodes.findAll().stream().map(this::episodeDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LesionEpisodeResponse> listEpisodesForPatient(Long patientId) {
        return episodes.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(this::episodeDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LesionEpisodeResponse> listEpisodesForRecord(Long recordId) {
        return episodes.findByMedicalRecordIdOrderByCreatedAtDesc(recordId).stream().map(this::episodeDto).toList();
    }

    @Transactional(readOnly = true)
    public LesionEpisodeResponse getEpisode(Long id) {
        return episodeDto(findEpisode(id));
    }

    @Transactional
    public LesionEpisodeResponse createEpisode(LesionEpisodeRequest request) {
        LesionEpisode saved = episodes.save(LesionEpisode.builder()
                .patientId(request.getPatientId())
                .medicalRecordId(request.getMedicalRecordId())
                .name(request.getName())
                .occurredAt(request.getOccurredAt())
                .circumstance(request.getOtherCircumstance() == null ? null : "Khac")
                .otherCircumstance(request.getOtherCircumstance())
                .medication(request.getMedication())
                .medicationDuration(request.getMedicationDuration())
                .timeUnit(request.getTimeUnit())
                .details(request.getDetails())
                .build());
        jdbc.update("UPDATE dottonthuongda SET context_tags = ? WHERE ma_dot_ton_thuong_da = ?", request.getCircumstance(), saved.getId());
        return episodeDto(saved);
    }

    @Transactional
    public LesionEpisodeResponse updateEpisode(Long id, LesionEpisodeRequest request) {
        LesionEpisode episode = findEpisode(id);
        episode.setPatientId(request.getPatientId());
        episode.setMedicalRecordId(request.getMedicalRecordId());
        episode.setName(request.getName());
        episode.setOccurredAt(request.getOccurredAt());
        episode.setCircumstance(request.getOtherCircumstance() == null ? null : "Khac");
        episode.setOtherCircumstance(request.getOtherCircumstance());
        episode.setMedication(request.getMedication());
        episode.setMedicationDuration(request.getMedicationDuration());
        episode.setTimeUnit(request.getTimeUnit());
        episode.setDetails(request.getDetails());
        LesionEpisode saved = episodes.save(episode);
        jdbc.update("UPDATE dottonthuongda SET context_tags = ? WHERE ma_dot_ton_thuong_da = ?", request.getCircumstance(), saved.getId());
        return episodeDto(saved);
    }

    @Transactional
    public void deleteEpisode(Long id) {
        LesionEpisode episode = findEpisode(id);
        photos.deletePhotosForEpisode(id);
        locations.deleteAll(locations.findByEpisode_Id(id));
        episodes.delete(episode);
    }

    @Transactional(readOnly = true)
    public List<LesionLocationResponse> listLocations(Long episodeId) {
        findEpisode(episodeId);
        return locations.findByEpisode_IdOrderByCreatedAtAsc(episodeId).stream().map(this::locationDto).toList();
    }

    @Transactional(readOnly = true)
    public LesionLocationResponse getLocation(Long id) {
        return locationDto(findLocation(id));
    }

    @Transactional
    public LesionLocationResponse createLocation(LesionLocationRequest request) {
        LesionEpisode episode = findEpisode(request.getEpisodeId());
        LesionLocation saved = locations.save(LesionLocation.builder()
                .episode(episode)
                .bodyPart(request.getBodyPart())
                .occurredAt(request.getOccurredAt())
                .relatedMedication(request.getRelatedMedication())
                .medicationDuration(request.getMedicationDuration())
                .circumstance(request.getOtherCircumstance() == null ? null : "Khac")
                .otherCircumstance(request.getOtherCircumstance())
                .timeUnit(request.getTimeUnit())
                .build());
        jdbc.update("UPDATE vitritonthuong SET context_tags = ? WHERE ma_vi_tri_ton_thuong = ?", request.getCircumstance(), saved.getId());
        return locationDto(saved);
    }

    @Transactional
    public LesionLocationResponse updateLocation(Long id, LesionLocationRequest request) {
        LesionLocation location = findLocation(id);
        location.setEpisode(findEpisode(request.getEpisodeId()));
        location.setBodyPart(request.getBodyPart());
        location.setOccurredAt(request.getOccurredAt());
        location.setRelatedMedication(request.getRelatedMedication());
        location.setMedicationDuration(request.getMedicationDuration());
        location.setCircumstance(request.getOtherCircumstance() == null ? null : "Khac");
        location.setOtherCircumstance(request.getOtherCircumstance());
        location.setTimeUnit(request.getTimeUnit());
        LesionLocation saved = locations.save(location);
        jdbc.update("UPDATE vitritonthuong SET context_tags = ? WHERE ma_vi_tri_ton_thuong = ?", request.getCircumstance(), saved.getId());
        return locationDto(saved);
    }

    @Transactional
    public void deleteLocation(Long id) {
        photos.deletePhotosForLocation(id);
        locations.delete(findLocation(id));
    }

    private LesionEpisode findEpisode(Long id) {
        return episodes.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Không tìm thấy đợt tổn thương: " + id));
    }

    private LesionLocation findLocation(Long id) {
        return locations.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Không tìm thấy vị trí tổn thương: " + id));
    }

    private LesionEpisodeResponse episodeDto(LesionEpisode episode) {
        return new LesionEpisodeResponse(
                episode.getId(), episode.getPatientId(), episode.getMedicalRecordId(), episode.getName(),
                episode.getOccurredAt(), jdbc.queryForObject("SELECT context_tags FROM dottonthuongda WHERE ma_dot_ton_thuong_da = ?", String.class, episode.getId()), episode.getOtherCircumstance(),
                episode.getMedication(), episode.getMedicationDuration(), episode.getTimeUnit(),
                episode.getDetails(), episode.getCreatedAt(), episode.getUpdatedAt());
    }

    private LesionLocationResponse locationDto(LesionLocation location) {
        return new LesionLocationResponse(
                location.getId(), location.getEpisode().getId(), location.getBodyPart(), location.getOccurredAt(),
                location.getRelatedMedication(), location.getMedicationDuration(), jdbc.queryForObject("SELECT context_tags FROM vitritonthuong WHERE ma_vi_tri_ton_thuong = ?", String.class, location.getId()),
                location.getOtherCircumstance(), location.getTimeUnit(), location.getCreatedAt(),
                location.getUpdatedAt());
    }
}
