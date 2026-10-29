package com.javeriana.zoofantastico;

import com.javeriana.zoofantastico.dto.CreatureRequest;
import com.javeriana.zoofantastico.dto.ZoneRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de las anotaciones de Bean Validation de los DTOs.
 * Mockito no dispara estas validaciones (las aplica @Valid en el controlador),
 * por eso se usa directamente el Validator de Jakarta.
 */
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    private static CreatureRequest validCreature() {
        return new CreatureRequest("Smaug", "Dragón", 15.5, 9, "healthy", 1L);
    }

    private static <T> Set<String> invalidFields(T request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    // ---------- CreatureRequest ----------

    @Test
    void testCreatureRequest_WhenAllFieldsValid_ShouldHaveNoViolations() {
        // Arrange
        CreatureRequest request = validCreature();

        // Act
        Set<ConstraintViolation<CreatureRequest>> violations = validator.validate(request);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void testCreatureRequest_WhenNameBlank_ShouldFailOnName(String name) {
        // Arrange
        CreatureRequest request = validCreature();
        request.setName(name);

        // Act
        Set<String> fields = invalidFields(request);

        // Assert
        assertEquals(Set.of("name"), fields);
    }

    @Test
    void testCreatureRequest_WhenSpeciesBlank_ShouldFailOnSpecies() {
        // Arrange
        CreatureRequest request = validCreature();
        request.setSpecies("");

        // Act
        Set<String> fields = invalidFields(request);

        // Assert
        assertEquals(Set.of("species"), fields);
    }

    @Test
    void testCreatureRequest_WhenSizeNegative_ShouldFailOnSize() {
        // Arrange
        CreatureRequest request = validCreature();
        request.setSize(-0.5);

        // Act
        Set<ConstraintViolation<CreatureRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<CreatureRequest> violation = violations.iterator().next();
        assertEquals("size", violation.getPropertyPath().toString());
        assertEquals("El tamaño (size) debe ser mayor o igual a 0", violation.getMessage());
    }

    @Test
    void testCreatureRequest_WhenSizeZero_ShouldBeValid() {
        // Arrange
        CreatureRequest request = validCreature();
        request.setSize(0);

        // Act
        Set<String> fields = invalidFields(request);

        // Assert
        assertTrue(fields.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11, -1, 100})
    void testCreatureRequest_WhenDangerLevelOutOfRange_ShouldFailOnDangerLevel(int dangerLevel) {
        // Arrange
        CreatureRequest request = validCreature();
        request.setDangerLevel(dangerLevel);

        // Act
        Set<String> fields = invalidFields(request);

        // Assert
        assertEquals(Set.of("dangerLevel"), fields);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10})
    void testCreatureRequest_WhenDangerLevelAtLimits_ShouldBeValid(int dangerLevel) {
        // Arrange
        CreatureRequest request = validCreature();
        request.setDangerLevel(dangerLevel);

        // Act
        Set<String> fields = invalidFields(request);

        // Assert
        assertTrue(fields.isEmpty());
    }

    @Test
    void testCreatureRequest_WhenHealthStatusBlank_ShouldFailOnHealthStatus() {
        // Arrange
        CreatureRequest request = validCreature();
        request.setHealthStatus(" ");

        // Act
        Set<String> fields = invalidFields(request);

        // Assert
        assertEquals(Set.of("healthStatus"), fields);
    }

    @Test
    void testCreatureRequest_WhenZoneIdNull_ShouldFailOnZoneId() {
        // Arrange
        CreatureRequest request = validCreature();
        request.setZoneId(null);

        // Act
        Set<String> fields = invalidFields(request);

        // Assert
        assertEquals(Set.of("zoneId"), fields);
    }

    @Test
    void testCreatureRequest_WhenEmpty_ShouldReportAllRequiredFields() {
        // Arrange
        CreatureRequest request = new CreatureRequest();

        // Act
        Set<String> fields = invalidFields(request);

        // Assert: size=0 es válido; dangerLevel=0 no lo es
        assertEquals(Set.of("name", "species", "dangerLevel", "healthStatus", "zoneId"), fields);
    }

    // ---------- ZoneRequest ----------

    @Test
    void testZoneRequest_WhenAllFieldsValid_ShouldHaveNoViolations() {
        // Arrange
        ZoneRequest request = new ZoneRequest("Bosque Mágico", null, 1);

        // Act
        Set<String> fields = invalidFields(request);

        // Assert
        assertTrue(fields.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void testZoneRequest_WhenNameBlank_ShouldFailOnName(String name) {
        // Arrange
        ZoneRequest request = new ZoneRequest(name, "desc", 5);

        // Act
        Set<String> fields = invalidFields(request);

        // Assert
        assertEquals(Set.of("name"), fields);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -3})
    void testZoneRequest_WhenCapacityBelowOne_ShouldFailOnCapacity(int capacity) {
        // Arrange
        ZoneRequest request = new ZoneRequest("Bosque Mágico", "desc", capacity);

        // Act
        Set<ConstraintViolation<ZoneRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(1, violations.size());
        assertEquals("La capacidad de la zona debe ser de al menos 1 criatura",
                violations.iterator().next().getMessage());
    }
}
