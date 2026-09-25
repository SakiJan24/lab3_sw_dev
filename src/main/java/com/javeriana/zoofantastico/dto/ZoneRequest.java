package com.javeriana.zoofantastico.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZoneRequest {

    @NotBlank(message = "El nombre de la zona es obligatorio")
    private String name;

    private String description;

    @Min(value = 1, message = "La capacidad de la zona debe ser de al menos 1 criatura")
    private int capacity;
}
