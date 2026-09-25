package com.javeriana.zoofantastico.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatureRequest {

    @NotBlank(message = "El nombre de la criatura es obligatorio")
    private String name;

    @NotBlank(message = "La especie es obligatoria")
    private String species;

    @Min(value = 0, message = "El tamaño (size) debe ser mayor o igual a 0")
    private double size;

    @Min(value = 1, message = "El nivel de peligro (dangerLevel) mínimo es 1")
    @Max(value = 10, message = "El nivel de peligro (dangerLevel) máximo es 10")
    private int dangerLevel;

    @NotBlank(message = "El estado de salud (healthStatus) es obligatorio")
    private String healthStatus;

    @NotNull(message = "El ID de la zona (zoneId) es obligatorio")
    private Long zoneId;
}

