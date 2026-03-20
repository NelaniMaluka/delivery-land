package com.deliveryland.backend.location;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String streetAddress;  // e.g., "123 Main St"

    @NotBlank
    private String city;           // e.g., "Johannesburg"

    @NotBlank
    private String province;       // e.g., "Gauteng"

    @NotBlank
    private String postalCode;     // e.g., "2000"

    @NotBlank
    private String country;        // e.g., "South Africa"

    @NotNull
    private Double latitude;       // e.g., -26.2041

    @NotNull
    private Double longitude;      // e.g., 28.0473
}