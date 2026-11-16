package com.example.carshop.dto;
import jakarta.validation.constraints.*;
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
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getManufacturerId() { return manufacturerId; }
    public void setManufacturerId(Long manufacturerId) { this.manufacturerId = manufacturerId; }
    public Long getDealershipId() { return dealershipId; }
    public void setDealershipId(Long dealershipId) { this.dealershipId = dealershipId; }
    public java.util.Set<@NotNull @Positive Long> getFeatureIds() { return featureIds; }
    public void setFeatureIds(java.util.Set<@NotNull @Positive Long> featureIds) { this.featureIds = featureIds; }
}
