package com.cnw.medical_system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:medicaltest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class MedicalSystemApplicationTests {
    private static final String CONTEXT_PATH = "/api/v1";

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void lesionEpisodeAndLocationCrudEndpointsWork() throws Exception {
        MvcResult createdEpisode = mockMvc.perform(post(CONTEXT_PATH + "/lesion-episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":1,"name":"Test episode","occurredAt":"2026-11-22T10:00:00Z","details":"Test episode"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(1))
                .andReturn();
        long episodeId = readId(createdEpisode);

        mockMvc.perform(get(CONTEXT_PATH + "/lesion-episodes/{id}", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details").value("Test episode"));

        MvcResult createdLocation = mockMvc.perform(post(CONTEXT_PATH + "/lesion-episodes/{episodeId}/locations", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"episodeId":0,"bodyPart":"arm","relatedMedication":"medicine","circumstance":"Test location"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.episodeId").value(episodeId))
                .andReturn();
        long locationId = readId(createdLocation);

        mockMvc.perform(get(CONTEXT_PATH + "/lesion-episodes/{episodeId}/locations", episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bodyPart").value("arm"));

        mockMvc.perform(put(CONTEXT_PATH + "/lesion-episodes/{id}", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":1,"name":"Updated episode","occurredAt":"2026-11-22T10:00:00Z","details":"Updated episode"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details").value("Updated episode"));

        mockMvc.perform(put(CONTEXT_PATH + "/lesion-episodes/locations/{id}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"episodeId":%d,"bodyPart":"leg","relatedMedication":"updated medicine","circumstance":"Updated location"}
                                """.formatted(episodeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyPart").value("leg"));

        mockMvc.perform(delete(CONTEXT_PATH + "/lesion-episodes/locations/{id}", locationId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(CONTEXT_PATH + "/lesion-episodes/{id}", episodeId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(CONTEXT_PATH + "/lesion-episodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get(CONTEXT_PATH + "/lesion-episodes/{id}", episodeId))
                .andExpect(status().isNotFound());
    }

    private long readId(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asLong();
    }
}

