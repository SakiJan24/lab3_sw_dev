package com.javeriana.zoofantastico;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javeriana.zoofantastico.dto.CreatureRequest;
import com.javeriana.zoofantastico.dto.ZoneRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    void testFullZooFlow() throws Exception {
        // 1. Create Zone (Capacity: 1)
        ZoneRequest zoneRequest = new ZoneRequest("Montaña de Hielo", "Zona de fénix de hielo", 1);
        MvcResult zoneResult = mockMvc.perform(post("/api/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Montaña de Hielo"))
                .andExpect(jsonPath("$.capacity").value(1))
                .andExpect(jsonPath("$.creatureCount").value(0))
                .andReturn();

        String zoneResponseBody = zoneResult.getResponse().getContentAsString();
        Long zoneId = objectMapper.readTree(zoneResponseBody).get("id").asLong();

        // 2. Create First Creature in Zone
        CreatureRequest creature1 = new CreatureRequest("Articuno", "Fénix", 3.2, 7, "healthy", zoneId);
        MvcResult creatureResult = mockMvc.perform(post("/api/creatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creature1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Articuno"))
                .andExpect(jsonPath("$.zoneId").value(zoneId))
                .andReturn();

        String creatureResponseBody = creatureResult.getResponse().getContentAsString();
        Long creatureId = objectMapper.readTree(creatureResponseBody).get("id").asLong();

        // 3. Verify Zone creatureCount updated to 1
        mockMvc.perform(get("/api/zones/" + zoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatureCount").value(1));

        // 4. Try to add 2nd Creature to Zone (Exceed Capacity -> 409 Conflict)
        CreatureRequest creature2 = new CreatureRequest("Frosty", "Fénix", 2.0, 5, "stable", zoneId);
        mockMvc.perform(post("/api/creatures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creature2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La zona 'Montaña de Hielo' ha alcanzado su capacidad máxima (1 criaturas)"));

        // 5. Try to delete Zone with creatures (-> 409 Conflict)
        mockMvc.perform(delete("/api/zones/" + zoneId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede eliminar la zona 'Montaña de Hielo' porque tiene 1 criatura(s) asociada(s)"));

        // 6. Update Creature to "critical" health status
        CreatureRequest updateCritical = new CreatureRequest("Articuno", "Fénix", 3.2, 7, "critical", zoneId);
        mockMvc.perform(put("/api/creatures/" + creatureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCritical)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthStatus").value("critical"));

        // 7. Try to delete Creature with "critical" status (-> 409 Conflict)
        mockMvc.perform(delete("/api/creatures/" + creatureId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede eliminar una criatura con estado de salud 'critical'"));

        // 8. Update Creature back to "recovering" and delete successfully
        CreatureRequest updateRecovering = new CreatureRequest("Articuno", "Fénix", 3.2, 7, "recovering", zoneId);
        mockMvc.perform(put("/api/creatures/" + creatureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRecovering)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/creatures/" + creatureId))
                .andExpect(status().isNoContent());

        // 9. Verify 404 for deleted creature
        mockMvc.perform(get("/api/creatures/" + creatureId))
                .andExpect(status().isNotFound());

        // 10. Now delete Zone (empty -> 204 No Content)
        mockMvc.perform(delete("/api/zones/" + zoneId))
                .andExpect(status().isNoContent());
    }

    @Test
    void testValidationErrors() throws Exception {
        // Invalid Creature Request (negative size, dangerLevel out of range)
        CreatureRequest invalidCreature = new CreatureRequest("", "", -5.0, 15, "", null);
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
