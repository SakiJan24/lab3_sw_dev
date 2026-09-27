package com.javeriana.zoofantastico;

import com.javeriana.zoofantastico.dto.CreatureRequest;
import com.javeriana.zoofantastico.dto.CreatureResponse;
import com.javeriana.zoofantastico.entity.Creature;
import com.javeriana.zoofantastico.entity.Zone;
import com.javeriana.zoofantastico.exception.BusinessRuleException;
import com.javeriana.zoofantastico.mapper.EntityMapper;
import com.javeriana.zoofantastico.repository.CreatureRepository;
import com.javeriana.zoofantastico.repository.ZoneRepository;
import com.javeriana.zoofantastico.service.CreatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatureServiceTest {

    @Mock
    private CreatureRepository creatureRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Spy
    private EntityMapper entityMapper;

    @InjectMocks
    private CreatureService creatureService;

    private Zone zone;
    private Creature creature;

    @BeforeEach
    void setUp() {
        zone = new Zone();
        zone.setId(1L);
        zone.setName("Cueva del Dragón");
        zone.setCapacity(2);

        creature = new Creature();
        creature.setId(10L);
        creature.setName("Smaug");
        creature.setSpecies("Dragón");
        creature.setSize(15.5);
        creature.setDangerLevel(9);
        creature.setHealthStatus("healthy");
        creature.setZone(zone);
    }

    @Test
    void createCreature_Success() {
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, 9, "healthy", 1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZone(zone)).thenReturn(0L);
        when(creatureRepository.save(any(Creature.class))).thenReturn(creature);

        CreatureResponse response = creatureService.createCreature(request);

        assertNotNull(response);
        assertEquals("Smaug", response.getName());
        assertEquals(1L, response.getZoneId());
        assertEquals("Cueva del Dragón", response.getZoneName());
    }

    @Test
    void createCreature_ExceedsCapacity_ThrowsBusinessRuleException() {
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, 9, "healthy", 1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZone(zone)).thenReturn(2L); // Capacity is 2

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> creatureService.createCreature(request));
        assertTrue(ex.getMessage().contains("alcanzado su capacidad máxima"));
    }

    @Test
    void deleteCreature_CriticalHealth_ThrowsBusinessRuleException() {
        creature.setHealthStatus("critical");
        when(creatureRepository.findById(10L)).thenReturn(Optional.of(creature));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> creatureService.deleteCreature(10L));
        assertTrue(ex.getMessage().contains("estado de salud 'critical'"));
        verify(creatureRepository, never()).delete(any());
    }

    @Test
    void deleteCreature_Healthy_Success() {
        when(creatureRepository.findById(10L)).thenReturn(Optional.of(creature));

        creatureService.deleteCreature(10L);

        verify(creatureRepository, times(1)).delete(creature);
    }
}

