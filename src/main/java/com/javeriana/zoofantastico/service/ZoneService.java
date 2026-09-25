package com.javeriana.zoofantastico.service;

import com.javeriana.zoofantastico.dto.ZoneRequest;
import com.javeriana.zoofantastico.dto.ZoneResponse;
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
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final CreatureRepository creatureRepository;
    private final EntityMapper entityMapper;

    @Transactional(readOnly = true)
    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll().stream()
                .map(zone -> {
                    long count = creatureRepository.countByZone(zone);
                    return entityMapper.toZoneResponse(zone, count);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ZoneResponse getZoneById(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));
        long count = creatureRepository.countByZone(zone);
        return entityMapper.toZoneResponse(zone, count);
    }

    public ZoneResponse createZone(ZoneRequest request) {
        Zone zone = entityMapper.toZoneEntity(request);
        Zone savedZone = zoneRepository.save(zone);
        return entityMapper.toZoneResponse(savedZone, 0);
    }

    public ZoneResponse updateZone(Long id, ZoneRequest request) {
        Zone existingZone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));

        long count = creatureRepository.countByZone(existingZone);
        if (request.getCapacity() < count) {
            throw new BusinessRuleException(
                    String.format("No se puede reducir la capacidad a %d porque la zona tiene actualmente %d criatura(s)", request.getCapacity(), count)
            );
        }

        existingZone.setName(request.getName());
        existingZone.setDescription(request.getDescription());
        existingZone.setCapacity(request.getCapacity());

        Zone updatedZone = zoneRepository.save(existingZone);
        return entityMapper.toZoneResponse(updatedZone, count);
    }


    public void deleteZone(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));
        
        long creatureCount = creatureRepository.countByZone(zone);
        if (creatureCount > 0) {
            throw new BusinessRuleException(
                    String.format("No se puede eliminar la zona '%s' porque tiene %d criatura(s) asociada(s)", zone.getName(), creatureCount)
            );
        }
        zoneRepository.delete(zone);
    }
}
