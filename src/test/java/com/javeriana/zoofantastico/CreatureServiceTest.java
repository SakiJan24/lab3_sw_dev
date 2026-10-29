package com.javeriana.zoofantastico;

import com.javeriana.zoofantastico.dto.CreatureRequest;
import com.javeriana.zoofantastico.dto.CreatureResponse;
import com.javeriana.zoofantastico.entity.Creature;
import com.javeriana.zoofantastico.entity.Zone;
import com.javeriana.zoofantastico.exception.BusinessRuleException;
import com.javeriana.zoofantastico.exception.GlobalExceptionHandler;
import com.javeriana.zoofantastico.exception.ResourceNotFoundException;
import com.javeriana.zoofantastico.mapper.EntityMapper;
import com.javeriana.zoofantastico.repository.CreatureRepository;
import com.javeriana.zoofantastico.repository.ZoneRepository;
import com.javeriana.zoofantastico.service.CreatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @Test
    void deleteAllCreatures_HasCritical_ThrowsBusinessRuleException() {
        Creature critical = new Creature();
        critical.setId(11L);
        critical.setName("Caos");
        critical.setHealthStatus("critical");
        when(creatureRepository.findAll()).thenReturn(List.of(creature, critical));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> creatureService.deleteAllCreatures());
        assertTrue(ex.getMessage().contains("critical"));
        verify(creatureRepository, never()).deleteAll(any());
    }

    @Test
    void deleteAllCreatures_AllHealthy_Success() {
        when(creatureRepository.findAll()).thenReturn(List.of(creature));

        creatureService.deleteAllCreatures();

        verify(creatureRepository, times(1)).deleteAll(List.of(creature));
    }

    // ---------- createCreature ----------

    @Test
    void testCreateCreature_ShouldReturnSavedCreature() {
        // Arrange
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, 9, "healthy", 1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZone(zone)).thenReturn(1L);
        when(creatureRepository.save(any(Creature.class))).thenAnswer(inv -> {
            Creature toSave = inv.getArgument(0);
            toSave.setId(10L);
            return toSave;
        });

        // Act
        CreatureResponse response = creatureService.createCreature(request);

        // Assert
        ArgumentCaptor<Creature> captor = ArgumentCaptor.forClass(Creature.class);
        verify(creatureRepository).save(captor.capture());
        Creature saved = captor.getValue();
        assertEquals("Smaug", saved.getName());
        assertEquals("Dragón", saved.getSpecies());
        assertEquals(9, saved.getDangerLevel());
        assertSame(zone, saved.getZone());
        assertEquals(10L, response.getId());
        assertEquals(1L, response.getZoneId());
        verify(zoneRepository).findById(1L);
        verify(creatureRepository).countByZone(zone);
    }

    @Test
    void testCreateCreature_WhenZoneIsFull_ShouldThrowConflict() {
        // Arrange
        CreatureRequest request = new CreatureRequest("Drogon", "Dragón", 12.0, 8, "healthy", 1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZone(zone)).thenReturn(2L);

        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> creatureService.createCreature(request));

        // Assert
        assertHttpStatus(ex, HttpStatus.CONFLICT);
        verify(creatureRepository, never()).save(any());
    }

    @Test
    void testCreateCreature_WhenZoneNotFound_ShouldThrowNotFound() {
        // Arrange
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, 9, "healthy", 99L);
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> creatureService.createCreature(request));

        // Assert
        assertHttpStatus(ex, HttpStatus.NOT_FOUND);
        assertTrue(ex.getMessage().contains("Zone"));
        verify(creatureRepository, never()).countByZone(any());
        verify(creatureRepository, never()).save(any());
    }

    @Test
    void testCreateCreature_WhenSizeIsNegative_ShouldThrowConflictWithoutTouchingRepositories() {
        // Arrange
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", -1.0, 9, "healthy", 1L);

        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> creatureService.createCreature(request));

        // Assert
        assertHttpStatus(ex, HttpStatus.CONFLICT);
        assertTrue(ex.getMessage().contains("size"));
        verifyNoInteractions(zoneRepository, creatureRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11, -5})
    void testCreateCreature_WhenDangerLevelOutOfRange_ShouldThrowConflict(int dangerLevel) {
        // Arrange
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, dangerLevel, "healthy", 1L);

        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> creatureService.createCreature(request));

        // Assert
        assertTrue(ex.getMessage().contains("dangerLevel"));
        verifyNoInteractions(zoneRepository, creatureRepository);
    }

    // ---------- getCreatureById ----------

    @Test
    void testGetCreatureById_ShouldReturnCreature() {
        // Arrange
        when(creatureRepository.findByIdWithZone(10L)).thenReturn(Optional.of(creature));

        // Act
        CreatureResponse response = creatureService.getCreatureById(10L);

        // Assert
        assertEquals(10L, response.getId());
        assertEquals("Smaug", response.getName());
        assertEquals("Cueva del Dragón", response.getZoneName());
        verify(creatureRepository).findByIdWithZone(10L);
    }

    @Test
    void testGetCreatureById_WhenNotFound_ShouldThrowNotFound() {
        // Arrange
        when(creatureRepository.findByIdWithZone(99L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> creatureService.getCreatureById(99L));

        // Assert
        assertHttpStatus(ex, HttpStatus.NOT_FOUND);
        assertTrue(ex.getMessage().contains("99"));
        verify(creatureRepository).findByIdWithZone(99L);
    }

    // ---------- updateCreature ----------

    @Test
    void testUpdateCreature_SameZone_ShouldUpdateFieldsWithoutCheckingCapacity() {
        // Arrange
        CreatureRequest request = new CreatureRequest("Smaug el Dorado", "Dragón", 16.0, 10, "injured", 1L);
        when(creatureRepository.findByIdWithZone(10L)).thenReturn(Optional.of(creature));
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.save(any(Creature.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CreatureResponse response = creatureService.updateCreature(10L, request);

        // Assert
        assertEquals("Smaug el Dorado", response.getName());
        assertEquals(16.0, response.getSize());
        assertEquals(10, response.getDangerLevel());
        assertEquals("injured", response.getHealthStatus());
        verify(creatureRepository, never()).countByZone(any());
        verify(creatureRepository).save(creature);
    }

    @Test
    void testUpdateCreature_ToZoneWithSpace_ShouldMoveCreature() {
        // Arrange
        Zone newZone = buildZone(2L, "Lago Encantado", 3);
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, 9, "healthy", 2L);
        when(creatureRepository.findByIdWithZone(10L)).thenReturn(Optional.of(creature));
        when(zoneRepository.findById(2L)).thenReturn(Optional.of(newZone));
        when(creatureRepository.countByZone(newZone)).thenReturn(1L);
        when(creatureRepository.save(any(Creature.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CreatureResponse response = creatureService.updateCreature(10L, request);

        // Assert
        assertEquals(2L, response.getZoneId());
        assertEquals("Lago Encantado", response.getZoneName());
        verify(creatureRepository).countByZone(newZone);
        verify(creatureRepository).save(creature);
    }

    @Test
    void testUpdateCreature_ToFullZone_ShouldThrowConflict() {
        // Arrange
        Zone fullZone = buildZone(2L, "Lago Encantado", 1);
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, 9, "healthy", 2L);
        when(creatureRepository.findByIdWithZone(10L)).thenReturn(Optional.of(creature));
        when(zoneRepository.findById(2L)).thenReturn(Optional.of(fullZone));
        when(creatureRepository.countByZone(fullZone)).thenReturn(1L);

        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> creatureService.updateCreature(10L, request));

        // Assert
        assertHttpStatus(ex, HttpStatus.CONFLICT);
        assertTrue(ex.getMessage().contains("Lago Encantado"));
        verify(creatureRepository, never()).save(any());
    }

    @Test
    void testUpdateCreature_WhenCreatureNotFound_ShouldThrowNotFound() {
        // Arrange
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, 9, "healthy", 1L);
        when(creatureRepository.findByIdWithZone(99L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> creatureService.updateCreature(99L, request));

        // Assert
        assertHttpStatus(ex, HttpStatus.NOT_FOUND);
        verifyNoInteractions(zoneRepository);
        verify(creatureRepository, never()).save(any());
    }

    @Test
    void testUpdateCreature_WhenZoneNotFound_ShouldThrowNotFound() {
        // Arrange
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, 9, "healthy", 99L);
        when(creatureRepository.findByIdWithZone(10L)).thenReturn(Optional.of(creature));
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> creatureService.updateCreature(10L, request));

        // Assert
        assertHttpStatus(ex, HttpStatus.NOT_FOUND);
        assertTrue(ex.getMessage().contains("Zone"));
        verify(creatureRepository, never()).save(any());
    }

    @Test
    void testUpdateCreature_WhenDangerLevelInvalid_ShouldThrowConflict() {
        // Arrange
        CreatureRequest request = new CreatureRequest("Smaug", "Dragón", 15.5, 11, "healthy", 1L);
        when(creatureRepository.findByIdWithZone(10L)).thenReturn(Optional.of(creature));

        // Act
        assertThrows(BusinessRuleException.class, () -> creatureService.updateCreature(10L, request));

        // Assert
        verifyNoInteractions(zoneRepository);
        verify(creatureRepository, never()).save(any());
    }

    // ---------- deleteCreature ----------

    @Test
    void testDeleteCreature_ShouldDeleteExistingCreature() {
        // Arrange
        creature.setHealthStatus("sick");
        when(creatureRepository.findById(10L)).thenReturn(Optional.of(creature));

        // Act
        creatureService.deleteCreature(10L);

        // Assert
        verify(creatureRepository).findById(10L);
        verify(creatureRepository).delete(creature);
    }

    @ParameterizedTest
    @ValueSource(strings = {"critical", "CRITICAL", "Critical"})
    void testDeleteCreature_WhenCritical_ShouldThrowConflict(String healthStatus) {
        // Arrange
        creature.setHealthStatus(healthStatus);
        when(creatureRepository.findById(10L)).thenReturn(Optional.of(creature));

        // Act
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> creatureService.deleteCreature(10L));

        // Assert
        assertHttpStatus(ex, HttpStatus.CONFLICT);
        verify(creatureRepository, never()).delete(any());
    }

    @Test
    void testDeleteCreature_WhenNotFound_ShouldThrowNotFound() {
        // Arrange
        when(creatureRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> creatureService.deleteCreature(99L));

        // Assert
        assertHttpStatus(ex, HttpStatus.NOT_FOUND);
        verify(creatureRepository, never()).delete(any());
    }

    // ---------- helpers ----------

    private static Zone buildZone(Long id, String name, int capacity) {
        Zone z = new Zone();
        z.setId(id);
        z.setName(name);
        z.setCapacity(capacity);
        return z;
    }

    /** Verifica el código HTTP que GlobalExceptionHandler asigna a la excepción (404 / 409). */
    private static void assertHttpStatus(RuntimeException ex, HttpStatus expected) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpStatusCode actual = ex instanceof BusinessRuleException bre
                ? handler.handleBusinessRuleException(bre).getStatusCode()
                : handler.handleResourceNotFoundException((ResourceNotFoundException) ex).getStatusCode();
        assertEquals(expected, actual);
    }
}

