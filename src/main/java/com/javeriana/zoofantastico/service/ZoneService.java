package com.javeriana.zoofantastico.service;

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
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final CreatureRepository creatureRepository;

    @Transactional(readOnly = true)
    public List<Zone> getAllZones() {
        return zoneRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Zone getZoneById(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));
    }

    public Zone createZone(Zone zone) {
        return zoneRepository.save(zone);
    }

    public Zone updateZone(Long id, Zone zoneDetails) {
        Zone existingZone = getZoneById(id);
        existingZone.setName(zoneDetails.getName());
        existingZone.setDescription(zoneDetails.getDescription());
        existingZone.setCapacity(zoneDetails.getCapacity());
        return zoneRepository.save(existingZone);
    }

    public void deleteZone(Long id) {
        Zone zone = getZoneById(id);
        long creatureCount = creatureRepository.countByZone(zone);
        if (creatureCount > 0) {
            throw new BusinessRuleException(
                    String.format("No se puede eliminar la zona '%s' porque tiene %d criatura(s) asociada(s)", zone.getName(), creatureCount)
            );
        }
        zoneRepository.delete(zone);
    }

    @Transactional(readOnly = true)
    public long countCreaturesByZone(Long zoneId) {
        Zone zone = getZoneById(zoneId);
        return creatureRepository.countByZone(zone);
    }
}
