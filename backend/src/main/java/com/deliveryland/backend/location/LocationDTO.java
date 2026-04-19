package com.deliveryland.backend.location;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocationDTO(

                @Schema(example = "123 Main St", description = "Street address of the location") @NotBlank(message = "Street address is required") String streetAddress,

                @Schema(example = "Johannesburg", description = "City name") @NotBlank(message = "City is required") String city,

                @Schema(example = "Gauteng", description = "Province or state") @NotBlank(message = "Province is required") String province,

                @Schema(example = "2000", description = "Postal or ZIP code") @NotBlank(message = "Postal code is required") String postalCode,

                @Schema(example = "South Africa", description = "Country name") @NotBlank(message = "Country is required") String country,

                @Schema(example = "-26.2041", description = "Latitude coordinate (-90 to 90)") @NotNull(message = "Latitude is required") Double latitude,

                @Schema(example = "28.0473", description = "Longitude coordinate (-180 to 180)") @NotNull(message = "Longitude is required") Double longitude) {

        public static LocationDTO from(Location location) {
                return new LocationDTO(
                                location.getStreetAddress(),
                                location.getCity(),
                                location.getProvince(),
                                location.getPostalCode(),
                                location.getCountry(),
                                location.getLatitude(),
                                location.getLongitude());
        }

}