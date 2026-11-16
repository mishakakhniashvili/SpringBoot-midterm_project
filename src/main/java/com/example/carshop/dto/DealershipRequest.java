package com.example.carshop.dto;
import jakarta.validation.constraints.*;
public record DealershipRequest(@NotBlank @Size(max=80) String name, @Size(max=500) String description,
                                @NotBlank @Email @Size(max=120) String email) {}
