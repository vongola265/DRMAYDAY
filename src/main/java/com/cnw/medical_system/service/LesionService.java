package com.cnw.medical_system.service;

import com.cnw.medical_system.dto.request.*;
import com.cnw.medical_system.dto.response.*;
import com.cnw.medical_system.entity.*;
import com.cnw.medical_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LesionService {
    private final LesionEpisodeRepository episodes;
    private final LesionLocationRepository locations;

    @Transactional(readOnly = true)
    public List<LesionEpisodeResponse> listEpisodes() { return episodes.findAll().stream().map(this::episodeDto).toList(); }
    @Transactional(readOnly = true)
    public LesionEpisodeResponse getEpisode(Long id) { return episodeDto(findEpisode(id)); }
    @Transactional
    public LesionEpisodeResponse createEpisode(LesionEpisodeRequest r) { return episodeDto(episodes.save(LesionEpisode.builder().patientId(r.getPatientId()).startDate(r.getStartDate()).endDate(r.getEndDate()).description(r.getDescription()).build())); }
    @Transactional
    public LesionEpisodeResponse updateEpisode(Long id, LesionEpisodeRequest r) { LesionEpisode e=findEpisode(id); e.setPatientId(r.getPatientId()); e.setStartDate(r.getStartDate()); e.setEndDate(r.getEndDate()); e.setDescription(r.getDescription()); return episodeDto(episodes.save(e)); }
    @Transactional
    public void deleteEpisode(Long id) { LesionEpisode e=findEpisode(id); locations.deleteAll(locations.findByEpisode_Id(id)); episodes.delete(e); }
    @Transactional(readOnly = true)
    public List<LesionLocationResponse> listLocations(Long episodeId) { findEpisode(episodeId); return locations.findByEpisode_Id(episodeId).stream().map(this::locationDto).toList(); }
    @Transactional
    public LesionLocationResponse createLocation(LesionLocationRequest r) { LesionEpisode e=findEpisode(r.getEpisodeId()); return locationDto(locations.save(LesionLocation.builder().episode(e).bodyPart(r.getBodyPart()).side(r.getSide()).description(r.getDescription()).build())); }
    @Transactional
    public LesionLocationResponse updateLocation(Long id, LesionLocationRequest r) { LesionLocation l=findLocation(id); l.setEpisode(findEpisode(r.getEpisodeId())); l.setBodyPart(r.getBodyPart()); l.setSide(r.getSide()); l.setDescription(r.getDescription()); return locationDto(locations.save(l)); }
    @Transactional
    public void deleteLocation(Long id) { locations.delete(findLocation(id)); }

    private LesionEpisode findEpisode(Long id) { return episodes.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt tổn thương: " + id)); }
    private LesionLocation findLocation(Long id) { return locations.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vị trí tổn thương: " + id)); }
    private LesionEpisodeResponse episodeDto(LesionEpisode e) { return new LesionEpisodeResponse(e.getId(), e.getPatientId(), e.getStartDate(), e.getEndDate(), e.getDescription()); }
    private LesionLocationResponse locationDto(LesionLocation l) { return new LesionLocationResponse(l.getId(), l.getEpisode().getId(), l.getBodyPart(), l.getSide(), l.getDescription()); }
}

