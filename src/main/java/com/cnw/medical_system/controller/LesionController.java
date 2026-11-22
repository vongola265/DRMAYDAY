package com.cnw.medical_system.controller;

import com.cnw.medical_system.dto.request.*;
import com.cnw.medical_system.dto.response.*;
import com.cnw.medical_system.service.LesionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/lesion-episodes")
@RequiredArgsConstructor
public class LesionController {
    private final LesionService service;

    @GetMapping public ResponseEntity<List<LesionEpisodeResponse>> getListLesionEpisodes() { return ResponseEntity.ok(service.listEpisodes()); }
    @GetMapping("/{id}") public ResponseEntity<LesionEpisodeResponse> getDetailLesionEpisode(@PathVariable Long id) { return ResponseEntity.ok(service.getEpisode(id)); }
    @PostMapping public ResponseEntity<LesionEpisodeResponse> setLesionEpisode(@Valid @RequestBody LesionEpisodeRequest request) { return ResponseEntity.ok(service.createEpisode(request)); }
    @PutMapping("/{id}") public ResponseEntity<LesionEpisodeResponse> editLesionEpisode(@PathVariable Long id, @Valid @RequestBody LesionEpisodeRequest request) { return ResponseEntity.ok(service.updateEpisode(id, request)); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> deleteLesionEpisode(@PathVariable Long id) { service.deleteEpisode(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{episodeId}/locations") public ResponseEntity<List<LesionLocationResponse>> getListLesionLocations(@PathVariable Long episodeId) { return ResponseEntity.ok(service.listLocations(episodeId)); }
    @PostMapping("/{episodeId}/locations") public ResponseEntity<LesionLocationResponse> setLesionLocation(@PathVariable Long episodeId, @Valid @RequestBody LesionLocationRequest request) { return ResponseEntity.ok(service.createLocation(new LesionLocationRequest(episodeId, request.getBodyPart(), request.getSide(), request.getDescription()))); }
    @PutMapping("/locations/{id}") public ResponseEntity<LesionLocationResponse> editLesionLocation(@PathVariable Long id, @Valid @RequestBody LesionLocationRequest request) { return ResponseEntity.ok(service.updateLocation(id, request)); }
    @DeleteMapping("/locations/{id}") public ResponseEntity<Void> deleteLesionLocation(@PathVariable Long id) { service.deleteLocation(id); return ResponseEntity.noContent().build(); }
}

