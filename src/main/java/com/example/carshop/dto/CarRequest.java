package com.example.carshop.dto;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
@Getter
@Setter
@NoArgsConstructor
public class CarRequest {
    @NotBlank @Size(max=100)
    private String model;
    @NotBlank @Pattern(regexp="[A-HJ-NPR-Za-hj-npr-z0-9]{17}", message="must be 17 VIN characters (no I, O or Q)")
    private String vin;
    @NotNull @Min(1886) @Max(2100)
    private Integer modelYear;
    @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2)
    private java.math.BigDecimal price;
    @Size(max=1000)
    private String description;
    @NotNull @Positive
    private Long manufacturerId;
    @NotNull @Positive
    private Long dealershipId;
    @NotNull @Size(min=1, max=30)
    private java.util.Set<@NotNull @Positive Long> featureIds;
}
