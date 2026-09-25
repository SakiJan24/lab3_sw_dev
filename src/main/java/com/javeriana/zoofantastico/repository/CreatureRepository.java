package com.javeriana.zoofantastico.repository;

import com.javeriana.zoofantastico.entity.Creature;
import com.javeriana.zoofantastico.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreatureRepository extends JpaRepository<Creature, Long> {
    long countByZone(Zone zone);
    long countByZoneId(Long zoneId);
    boolean existsByZoneId(Long zoneId);
}
