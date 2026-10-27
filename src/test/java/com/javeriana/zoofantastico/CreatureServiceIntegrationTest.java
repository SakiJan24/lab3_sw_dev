package com.javeriana.zoofantastico;

import com.javeriana.zoofantastico.dto.CreatureRequest;
import com.javeriana.zoofantastico.dto.CreatureResponse;
import com.javeriana.zoofantastico.entity.Creature;
import com.javeriana.zoofantastico.entity.Zone;
import com.javeriana.zoofantastico.repository.CreatureRepository;
import com.javeriana.zoofantastico.repository.ZoneRepository;
import com.javeriana.zoofantastico.service.CreatureService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreatureServiceIntegrationTest {

    @Autowired
    private CreatureService creatureService;

    @Autowired
    private CreatureRepository creatureRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private EntityManager entityManager;

    private Long zoneId;

    @BeforeEach
    void setUp() {
        // Cada criatura necesita una zona existente.
        Zone zone = new Zone();
        zone.setName("Bosque encantado");
        zone.setDescription("Zona de prueba");
        zone.setCapacity(10);

        Zone savedZone = zoneRepository.save(zone);
        zoneId = savedZone.getId();
    }

    private CreatureRequest crearSolicitud(String name, String healthStatus) {
        return new CreatureRequest(
                name,
                "Unicornio",
                2.5,
                5,
                healthStatus,
                zoneId
        );
    }

    @Test
    void crearCriatura_debeGuardarseEnLaBaseDeDatos() {
        // Ejecutar: crear la criatura mediante el servicio.
        CreatureResponse created = creatureService.createCreature(
                crearSolicitud("Luna", "healthy")
        );

        // Enviar los cambios a H2 y limpiar la caché de JPA.
        entityManager.flush();
        entityManager.clear();

        // Consultar de nuevo la criatura en la base de datos.
        Creature found = creatureRepository.findById(created.getId())
                .orElseThrow();

        assertEquals("Luna", found.getName());
        assertEquals("Unicornio", found.getSpecies());
        assertEquals(5, found.getDangerLevel());
        assertEquals(zoneId, found.getZone().getId());
    }

    @Test
    void actualizarCriatura_debeGuardarLosCambiosEnLaBaseDeDatos() {
        // Preparar: crear una criatura.
        CreatureResponse created = creatureService.createCreature(
                crearSolicitud("Luna", "healthy")
        );

        // Ejecutar: actualizarla mediante el servicio.
        creatureService.updateCreature(
                created.getId(),
                crearSolicitud("Estrella", "recovering")
        );

        entityManager.flush();
        entityManager.clear();

        // Comprobar que los cambios se guardaron.
        Creature found = creatureRepository.findById(created.getId())
                .orElseThrow();

        assertEquals("Estrella", found.getName());
        assertEquals("recovering", found.getHealthStatus());
    }

    @Test
    void eliminarCriatura_debeDesaparecerDeLaBaseDeDatos() {
        // Preparar: crear una criatura que no esté en estado crítico.
        CreatureResponse created = creatureService.createCreature(
                crearSolicitud("Luna", "healthy")
        );

        Long creatureId = created.getId();

        // Ejecutar: eliminarla mediante el servicio.
        creatureService.deleteCreature(creatureId);

        entityManager.flush();
        entityManager.clear();

        // Comprobar que ya no existe.
        assertFalse(creatureRepository.existsById(creatureId));
    }
}