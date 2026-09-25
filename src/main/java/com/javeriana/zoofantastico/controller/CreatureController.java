package com.javeriana.zoofantastico.controller;

import com.javeriana.zoofantastico.dto.CreatureRequest;
import com.javeriana.zoofantastico.dto.CreatureResponse;
import com.javeriana.zoofantastico.service.CreatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creatures")
@RequiredArgsConstructor
public class CreatureController {

    private final CreatureService creatureService;

    @GetMapping
    public ResponseEntity<List<CreatureResponse>> getAllCreatures() {
        List<CreatureResponse> creatures = creatureService.getAllCreatures();
        return ResponseEntity.ok(creatures);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreatureResponse> getCreatureById(@PathVariable Long id) {
        CreatureResponse creature = creatureService.getCreatureById(id);
        return ResponseEntity.ok(creature);
    }

    @PostMapping
    public ResponseEntity<CreatureResponse> createCreature(@Valid @RequestBody CreatureRequest request) {
        CreatureResponse createdCreature = creatureService.createCreature(request);
        return new ResponseEntity<>(createdCreature, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreatureResponse> updateCreature(
            @PathVariable Long id,
            @Valid @RequestBody CreatureRequest request) {
        CreatureResponse updatedCreature = creatureService.updateCreature(id, request);
        return ResponseEntity.ok(updatedCreature);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreature(@PathVariable Long id) {
        creatureService.deleteCreature(id);
        return ResponseEntity.noContent().build();
    }
}
