package com.example.carshop.dto;
import jakarta.validation.constraints.*;
public record ReferenceRequest(@NotBlank @Size(max=80) String name, @Size(max=500) String description) {}
