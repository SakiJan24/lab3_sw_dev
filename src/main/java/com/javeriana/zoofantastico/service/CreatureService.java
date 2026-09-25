package com.javeriana.zoofantastico.service;

import com.javeriana.zoofantastico.entity.Creature;
import com.javeriana.zoofantastico.entity.Zone;
import com.javeriana.zoofantastico.exception.BusinessRuleException;
import com.javeriana.zoofantastico.exception.ResourceNotFoundException;
import com.javeriana.zoofantastico.repository.CreatureRepository;
import com.javeriana.zoofantastico.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CreatureService {

    private final CreatureRepository creatureRepository;
    private final ZoneRepository zoneRepository;

    @Transactional(readOnly = true)
    public List<Creature> getAllCreatures() {
        return creatureRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Creature getCreatureById(Long id) {
        return creatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Creature", "id", id));
    }

    public Creature createCreature(Creature creature, Long zoneId) {
        validateCreatureData(creature);

        if (zoneId != null) {
            Zone zone = zoneRepository.findById(zoneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", zoneId));
            validateZoneCapacityForNewCreature(zone);
            creature.setZone(zone);
        }

        return creatureRepository.save(creature);
    }

    public Creature updateCreature(Long id, Creature creatureDetails, Long zoneId) {
        Creature existingCreature = getCreatureById(id);
        validateCreatureData(creatureDetails);

        existingCreature.setName(creatureDetails.getName());
        existingCreature.setSpecies(creatureDetails.getSpecies());
        existingCreature.setSize(creatureDetails.getSize());
        existingCreature.setDangerLevel(creatureDetails.getDangerLevel());
        existingCreature.setHealthStatus(creatureDetails.getHealthStatus());

        if (zoneId != null) {
            Zone newZone = zoneRepository.findById(zoneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", zoneId));

            // Si cambió de zona, validar capacidad de la nueva zona
            Zone currentZone = existingCreature.getZone();
            if (currentZone == null || !currentZone.getId().equals(newZone.getId())) {
                validateZoneCapacityForNewCreature(newZone);
            }
            existingCreature.setZone(newZone);
        } else {
            existingCreature.setZone(null);
        }

        return creatureRepository.save(existingCreature);
    }

    public void deleteCreature(Long id) {
        Creature creature = getCreatureById(id);
        if ("critical".equalsIgnoreCase(creature.getHealthStatus())) {
            throw new BusinessRuleException("No se puede eliminar una criatura con estado de salud 'critical'");
        }
        creatureRepository.delete(creature);
    }

    private void validateCreatureData(Creature creature) {
        if (creature.getSize() < 0) {
            throw new BusinessRuleException("El tamaño (size) de la criatura no puede ser negativo");
        }
        if (creature.getDangerLevel() < 1 || creature.getDangerLevel() > 10) {
            throw new BusinessRuleException("El nivel de peligro (dangerLevel) debe estar entre 1 y 10");
        }
    }

    private void validateZoneCapacityForNewCreature(Zone zone) {
        long currentCreatures = creatureRepository.countByZone(zone);
        if (currentCreatures >= zone.getCapacity()) {
            throw new BusinessRuleException(
                    String.format("La zona '%s' ha alcanzado su capacidad máxima (%d criaturas)", zone.getName(), zone.getCapacity())
            );
        }
    }
}
