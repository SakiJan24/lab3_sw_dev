package com.javeriana.zoofantastico.repository;

import com.javeriana.zoofantastico.entity.Creature;
import com.javeriana.zoofantastico.entity.Zone;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreatureRepository extends JpaRepository<Creature, Long> {

    /**
     * Solución al problema N+1:
     * Al consultar todas las criaturas, Hibernate ejecutaba 1 consulta inicial para obtener las criaturas
     * y luego N consultas adicionales para cargar Lazy la entidad Zone asociada a cada una.
     * Mediante @EntityGraph(attributePaths = {"zone"}), Spring Data JPA realiza un LEFT OUTER JOIN implícito,
     * cargando la entidad Zone en la misma consulta inicial de base de datos.
     */
    @Override
    @EntityGraph(attributePaths = {"zone"})
    List<Creature> findAll();

    /**
     * Consulta por ID optimizada con JOIN FETCH de la zona asociada para evitar N+1 o LazyInitializationException.
     */
    @Query("SELECT c FROM Creature c LEFT JOIN FETCH c.zone WHERE c.id = :id")
    Optional<Creature> findByIdWithZone(Long id);

    long countByZone(Zone zone);
    long countByZoneId(Long zoneId);
    boolean existsByZoneId(Long zoneId);
}
