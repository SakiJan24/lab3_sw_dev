package com.javeriana.zoofantastico.mapper;

import com.javeriana.zoofantastico.dto.CreatureRequest;
import com.javeriana.zoofantastico.dto.CreatureResponse;
import com.javeriana.zoofantastico.dto.ZoneRequest;
import com.javeriana.zoofantastico.dto.ZoneResponse;
import com.javeriana.zoofantastico.entity.Creature;
import com.javeriana.zoofantastico.entity.Zone;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public CreatureResponse toCreatureResponse(Creature creature) {
        if (creature == null) {
            return null;
        }
        return CreatureResponse.builder()
                .id(creature.getId())
                .name(creature.getName())
                .species(creature.getSpecies())
                .size(creature.getSize())
                .dangerLevel(creature.getDangerLevel())
                .healthStatus(creature.getHealthStatus())
                .zoneId(creature.getZone() != null ? creature.getZone().getId() : null)
                .zoneName(creature.getZone() != null ? creature.getZone().getName() : null)
                .build();
    }

    public Creature toCreatureEntity(CreatureRequest request) {
        if (request == null) {
            return null;
        }
        Creature creature = new Creature();
        creature.setName(request.getName());
        creature.setSpecies(request.getSpecies());
        creature.setSize(request.getSize());
        creature.setDangerLevel(request.getDangerLevel());
        creature.setHealthStatus(request.getHealthStatus());
        return creature;
    }

    public ZoneResponse toZoneResponse(Zone zone, long creatureCount) {
        if (zone == null) {
            return null;
        }
        return ZoneResponse.builder()
                .id(zone.getId())
                .name(zone.getName())
                .description(zone.getDescription())
                .capacity(zone.getCapacity())
                .creatureCount(creatureCount)
                .build();
    }

    public Zone toZoneEntity(ZoneRequest request) {
        if (request == null) {
            return null;
        }
        Zone zone = new Zone();
        zone.setName(request.getName());
        zone.setDescription(request.getDescription());
        zone.setCapacity(request.getCapacity());
        return zone;
    }
}

