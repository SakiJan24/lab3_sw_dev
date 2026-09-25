package com.javeriana.zoofantastico;

import com.javeriana.zoofantastico.dto.ZoneRequest;
import com.javeriana.zoofantastico.dto.ZoneResponse;
import com.javeriana.zoofantastico.entity.Zone;
import com.javeriana.zoofantastico.exception.BusinessRuleException;
import com.javeriana.zoofantastico.exception.ResourceNotFoundException;
import com.javeriana.zoofantastico.mapper.EntityMapper;
import com.javeriana.zoofantastico.repository.CreatureRepository;
import com.javeriana.zoofantastico.repository.ZoneRepository;
import com.javeriana.zoofantastico.service.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private CreatureRepository creatureRepository;

    @Spy
    private EntityMapper entityMapper;

    @InjectMocks
    private ZoneService zoneService;

    private Zone sampleZone;

    @BeforeEach
    void setUp() {
        sampleZone = new Zone();
        sampleZone.setId(1L);
        sampleZone.setName("Bosque Mágico");
        sampleZone.setDescription("Zona de criaturas del bosque");
        sampleZone.setCapacity(5);
    }

    @Test
    void createZone_Success() {
        ZoneRequest request = new ZoneRequest("Bosque Mágico", "Zona de criaturas del bosque", 5);
        when(zoneRepository.save(any(Zone.class))).thenReturn(sampleZone);

        ZoneResponse response = zoneService.createZone(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Bosque Mágico", response.getName());
        assertEquals(0, response.getCreatureCount());
        verify(zoneRepository, times(1)).save(any(Zone.class));
    }

    @Test
    void getZoneById_Success() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(2L);

        ZoneResponse response = zoneService.getZoneById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(2L, response.getCreatureCount());
    }

    @Test
    void getZoneById_NotFound_ThrowsException() {
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> zoneService.getZoneById(99L));
    }

    @Test
    void deleteZone_WithAssociatedCreatatures_ThrowsBusinessRuleException() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(1L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> zoneService.deleteZone(1L));
        assertTrue(ex.getMessage().contains("tiene 1 criatura(s) asociada(s)"));
        verify(zoneRepository, never()).delete(any());
    }

    @Test
    void deleteZone_WithoutCreatatures_Success() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(0L);

        zoneService.deleteZone(1L);

        verify(zoneRepository, times(1)).delete(sampleZone);
    }
}
