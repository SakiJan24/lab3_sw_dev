package com.javeriana.zoofantastico;

import com.javeriana.zoofantastico.dto.ZoneRequest;
import com.javeriana.zoofantastico.dto.ZoneResponse;
import com.javeriana.zoofantastico.entity.Zone;
import com.javeriana.zoofantastico.exception.BusinessRuleException;
import com.javeriana.zoofantastico.exception.GlobalExceptionHandler;
import com.javeriana.zoofantastico.exception.ResourceNotFoundException;
import com.javeriana.zoofantastico.mapper.EntityMapper;
import com.javeriana.zoofantastico.repository.CreatureRepository;
import com.javeriana.zoofantastico.repository.ZoneRepository;
import com.javeriana.zoofantastico.service.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

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
    void deleteZone_WithAssociatedCreatures_ThrowsBusinessRuleException() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(1L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> zoneService.deleteZone(1L));
        assertTrue(ex.getMessage().contains("tiene 1 criatura(s) asociada(s)"));
        verify(zoneRepository, never()).delete(any());
    }

    @Test
    void deleteZone_WithoutCreatures_Success() {
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(0L);

        zoneService.deleteZone(1L);

        verify(zoneRepository, times(1)).delete(sampleZone);
    }

    @Test
    void deleteAllZones_WithCreatures_ThrowsBusinessRuleException() {
        when(zoneRepository.findAll()).thenReturn(List.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(1L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> zoneService.deleteAllZones());
        assertTrue(ex.getMessage().contains("criatura(s) asociada(s)"));
        verify(zoneRepository, never()).deleteAll(any());
    }

    @Test
    void deleteAllZones_EmptyZones_Success() {
        when(zoneRepository.findAll()).thenReturn(List.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(0L);

        zoneService.deleteAllZones();

        verify(creatureRepository, times(1)).countByZone(sampleZone);
        verify(zoneRepository, times(1)).deleteAll(List.of(sampleZone));
    }

    // ---------- createZone ----------

    @Test
    void testCreateZone_ShouldReturnSavedZoneWithZeroCreatures() {
        // Arrange
        ZoneRequest request = new ZoneRequest("Lago Encantado", "Zona acuática", 10);
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> {
            Zone toSave = inv.getArgument(0);
            toSave.setId(2L);
            return toSave;
        });

        // Act
        ZoneResponse response = zoneService.createZone(request);

        // Assert
        ArgumentCaptor<Zone> captor = ArgumentCaptor.forClass(Zone.class);
        verify(zoneRepository).save(captor.capture());
        assertEquals("Lago Encantado", captor.getValue().getName());
        assertEquals("Zona acuática", captor.getValue().getDescription());
        assertEquals(10, captor.getValue().getCapacity());
        assertEquals(2L, response.getId());
        assertEquals(0, response.getCreatureCount());
        verifyNoInteractions(creatureRepository);
    }

    // ---------- getZoneById ----------

    @Test
    void testGetZoneById_ShouldReturnZoneWithCreatureCount() {
        // Arrange
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(3L);

        // Act
        ZoneResponse response = zoneService.getZoneById(1L);

        // Assert
        assertEquals("Bosque Mágico", response.getName());
        assertEquals(5, response.getCapacity());
        assertEquals(3L, response.getCreatureCount());
        verify(zoneRepository).findById(1L);
        verify(creatureRepository).countByZone(sampleZone);
    }

    @Test
    void testGetZoneById_WhenNotFound_ShouldThrowNotFound() {
        // Arrange
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> zoneService.getZoneById(99L));

        // Assert
        assertHttpStatus(ex, HttpStatus.NOT_FOUND);
        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(creatureRepository);
    }

    // ---------- updateZone ----------

    @Test
    void testUpdateZone_ShouldUpdateFields() {
        // Arrange
        ZoneRequest request = new ZoneRequest("Bosque Oscuro", "Nueva descripción", 8);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(2L);
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ZoneResponse response = zoneService.updateZone(1L, request);

        // Assert
        assertEquals("Bosque Oscuro", response.getName());
        assertEquals("Nueva descripción", response.getDescription());
        assertEquals(8, response.getCapacity());
        assertEquals(2L, response.getCreatureCount());
        verify(zoneRepository).save(sampleZone);
    }

    @Test
    void testUpdateZone_WhenCapacityEqualsCreatureCount_ShouldUpdate() {
        // Arrange
        ZoneRequest request = new ZoneRequest("Bosque Mágico", null, 2);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(2L);
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ZoneResponse response = zoneService.updateZone(1L, request);

        // Assert
        assertEquals(2, response.getCapacity());
        verify(zoneRepository).save(sampleZone);
    }

    @Test
    void testUpdateZone_WhenCapacityBelowCreatureCount_ShouldThrowConflict() {
        // Arrange
        ZoneRequest request = new ZoneRequest("Bosque Mágico", null, 1);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(3L);

        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> zoneService.updateZone(1L, request));

        // Assert
        assertHttpStatus(ex, HttpStatus.CONFLICT);
        assertEquals(5, sampleZone.getCapacity());
        verify(zoneRepository, never()).save(any());
    }

    @Test
    void testUpdateZone_WhenNotFound_ShouldThrowNotFound() {
        // Arrange
        ZoneRequest request = new ZoneRequest("Bosque Mágico", null, 5);
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> zoneService.updateZone(99L, request));

        // Assert
        assertHttpStatus(ex, HttpStatus.NOT_FOUND);
        verify(zoneRepository, never()).save(any());
        verifyNoInteractions(creatureRepository);
    }

    // ---------- deleteZone ----------

    @Test
    void testDeleteZone_ShouldDeleteEmptyZone() {
        // Arrange
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(0L);

        // Act
        zoneService.deleteZone(1L);

        // Assert
        verify(creatureRepository).countByZone(sampleZone);
        verify(zoneRepository).delete(sampleZone);
    }

    @Test
    void testDeleteZone_WhenHasCreatures_ShouldThrowConflict() {
        // Arrange
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(sampleZone));
        when(creatureRepository.countByZone(sampleZone)).thenReturn(4L);

        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> zoneService.deleteZone(1L));

        // Assert
        assertHttpStatus(ex, HttpStatus.CONFLICT);
        assertTrue(ex.getMessage().contains("Bosque Mágico"));
        verify(zoneRepository, never()).delete(any());
    }

    @Test
    void testDeleteZone_WhenNotFound_ShouldThrowNotFound() {
        // Arrange
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> zoneService.deleteZone(99L));

        // Assert
        assertHttpStatus(ex, HttpStatus.NOT_FOUND);
        verify(zoneRepository, never()).delete(any());
        verifyNoInteractions(creatureRepository);
    }

    // ---------- helpers ----------

    /** Verifica el código HTTP que GlobalExceptionHandler asigna a la excepción (404 / 409). */
    private static void assertHttpStatus(RuntimeException ex, HttpStatus expected) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpStatusCode actual = ex instanceof BusinessRuleException bre
                ? handler.handleBusinessRuleException(bre).getStatusCode()
                : handler.handleResourceNotFoundException((ResourceNotFoundException) ex).getStatusCode();
        assertEquals(expected, actual);
    }
}
