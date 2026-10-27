package com.javeriana.zoofantastico;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javeriana.zoofantastico.dto.CreatureRequest;
import com.javeriana.zoofantastico.dto.ZoneRequest;
import com.javeriana.zoofantastico.repository.CreatureRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ZooIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreatureRepository creatureRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testFullZooFlow() throws Exception {

        // 1. Crear una zona con capacidad para una criatura.
        ZoneRequest zoneRequest = new ZoneRequest(
                "Montaña de Hielo",
                "Zona de fénix de hielo",
                1
        );

        MvcResult zoneResult = mockMvc.perform(post("/api/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Montaña de Hielo"))
                .andExpect(jsonPath("$.capacity").value(1))
                .andExpect(jsonPath("$.creatureCount").value(0))
                .andReturn();

        Long zoneId = objectMapper.readTree(
                zoneResult.getResponse().getContentAsString()
        ).get("id").asLong();

        // 2. Crear una criatura en la zona.
        CreatureRequest creature1 = new CreatureRequest(
                "Articuno",
                "Fénix",
                3.2,
                7,
                "healthy",
                zoneId
        );

        MvcResult creatureResult = mockMvc.perform(post("/api/creatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creature1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Articuno"))
                .andExpect(jsonPath("$.zoneId").value(zoneId))
                .andExpect(jsonPath("$.zoneName").value("Montaña de Hielo"))
                .andReturn();

        Long creatureId = objectMapper.readTree(
                creatureResult.getResponse().getContentAsString()
        ).get("id").asLong();

        // Verificar directamente en H2 que la criatura fue guardada.
        entityManager.flush();
        entityManager.clear();

        var guardada = creatureRepository.findById(creatureId);

        assertTrue(
                guardada.isPresent(),
                "La criatura creada debe existir en H2"
        );

        assertEquals(
                "Articuno",
                guardada.orElseThrow().getName(),
                "El nombre guardado debe ser Articuno"
        );

        // 3. Verificar que la zona ahora tiene una criatura.
        mockMvc.perform(get("/api/zones/" + zoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatureCount").value(1));

        // 4. Intentar superar la capacidad de la zona.
        CreatureRequest creature2 = new CreatureRequest(
                "Frosty",
                "Fénix",
                2.0,
                5,
                "stable",
                zoneId
        );

        mockMvc.perform(post("/api/creatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creature2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "La zona 'Montaña de Hielo' ha alcanzado su capacidad máxima (1 criaturas)"
                ));

        // 5. Intentar eliminar una zona que todavía tiene criaturas.
        mockMvc.perform(delete("/api/zones/" + zoneId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "No se puede eliminar la zona 'Montaña de Hielo' porque tiene 1 criatura(s) asociada(s)"
                ));

        // 6. Actualizar el estado de salud de la criatura a critical.
        CreatureRequest updateCritical = new CreatureRequest(
                "Articuno",
                "Fénix",
                3.2,
                7,
                "critical",
                zoneId
        );

        mockMvc.perform(put("/api/creatures/" + creatureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCritical)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthStatus").value("critical"));

        // Verificar directamente en H2 que el cambio fue guardado.
        entityManager.flush();
        entityManager.clear();

        var actualizada = creatureRepository.findById(creatureId);

        assertTrue(
                actualizada.isPresent(),
                "La criatura debe seguir existiendo después de actualizarla"
        );

        assertEquals(
                "critical",
                actualizada.orElseThrow().getHealthStatus(),
                "El estado actualizado debe estar guardado en H2"
        );

        // 7. Intentar eliminar una criatura en estado crítico.
        mockMvc.perform(delete("/api/creatures/" + creatureId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "No se puede eliminar una criatura con estado de salud 'critical'"
                ));

        // 8. Cambiar el estado a recovering y eliminar la criatura.
        CreatureRequest updateRecovering = new CreatureRequest(
                "Articuno",
                "Fénix",
                3.2,
                7,
                "recovering",
                zoneId
        );

        mockMvc.perform(put("/api/creatures/" + creatureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRecovering)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthStatus").value("recovering"));

        mockMvc.perform(delete("/api/creatures/" + creatureId))
                .andExpect(status().isNoContent());

        // Verificar directamente en H2 que la criatura ya no existe.
        entityManager.flush();
        entityManager.clear();

        assertFalse(
                creatureRepository.existsById(creatureId),
                "La criatura eliminada ya no debe existir en H2"
        );

        // 9. Consultar la criatura eliminada debe devolver 404.
        mockMvc.perform(get("/api/creatures/" + creatureId))
                .andExpect(status().isNotFound());

        // 10. Eliminar la zona ahora que está vacía.
        mockMvc.perform(delete("/api/zones/" + zoneId))
                .andExpect(status().isNoContent());
    }

    @Test
    void testValidationErrors() throws Exception {

        // Enviar campos vacíos y valores fuera de los límites permitidos.
        CreatureRequest invalidCreature = new CreatureRequest(
                "",
                "",
                -5.0,
                15,
                "",
                null
        );

        mockMvc.perform(post("/api/creatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCreature)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.species").exists())
                .andExpect(jsonPath("$.details.size").exists())
                .andExpect(jsonPath("$.details.dangerLevel").exists())
                .andExpect(jsonPath("$.details.healthStatus").exists())
                .andExpect(jsonPath("$.details.zoneId").exists());
    }
}
