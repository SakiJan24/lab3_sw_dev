package com.javeriana.zoofantastico.service;

import com.javeriana.zoofantastico.dto.CreatureRequest;
import com.javeriana.zoofantastico.dto.CreatureResponse;
import com.javeriana.zoofantastico.entity.Creature;
import com.javeriana.zoofantastico.entity.Zone;
import com.javeriana.zoofantastico.exception.BusinessRuleException;
import com.javeriana.zoofantastico.exception.ResourceNotFoundException;
import com.javeriana.zoofantastico.mapper.EntityMapper;
import com.javeriana.zoofantastico.repository.CreatureRepository;
import com.javeriana.zoofantastico.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CreatureService {

    private final CreatureRepository creatureRepository;
    private final ZoneRepository zoneRepository;
    private final EntityMapper entityMapper;

    @Transactional(readOnly = true)
    public List<CreatureResponse> getAllCreatures() {
        return creatureRepository.findAll().stream()
                .map(entityMapper::toCreatureResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CreatureResponse getCreatureById(Long id) {
        Creature creature = creatureRepository.findByIdWithZone(id)
                .orElseThrow(() -> new ResourceNotFoundException("Creature", "id", id));
        return entityMapper.toCreatureResponse(creature);
    }

    public CreatureResponse createCreature(CreatureRequest request) {
        Creature creature = entityMapper.toCreatureEntity(request);
        validateCreatureData(creature);

        if (request.getZoneId() != null) {
            Zone zone = zoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.getZoneId()));
            validateZoneCapacityForNewCreature(zone);
            creature.setZone(zone);
        }

        Creature savedCreature = creatureRepository.save(creature);
        return entityMapper.toCreatureResponse(savedCreature);
    }

    public CreatureResponse updateCreature(Long id, CreatureRequest request) {
        Creature existingCreature = creatureRepository.findByIdWithZone(id)
                .orElseThrow(() -> new ResourceNotFoundException("Creature", "id", id));

        Creature updatedData = entityMapper.toCreatureEntity(request);
        validateCreatureData(updatedData);

        existingCreature.setName(request.getName());
        existingCreature.setSpecies(request.getSpecies());
        existingCreature.setSize(request.getSize());
        existingCreature.setDangerLevel(request.getDangerLevel());
        existingCreature.setHealthStatus(request.getHealthStatus());

        if (request.getZoneId() != null) {
            Zone newZone = zoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.getZoneId()));

            Zone currentZone = existingCreature.getZone();
            if (currentZone == null || !currentZone.getId().equals(newZone.getId())) {
                validateZoneCapacityForNewCreature(newZone);
            }
            existingCreature.setZone(newZone);
        } else {
            existingCreature.setZone(null);
        }

        Creature savedCreature = creatureRepository.save(existingCreature);
        return entityMapper.toCreatureResponse(savedCreature);
    }

    public void deleteCreature(Long id) {
        Creature creature = creatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Creature", "id", id));

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
