package com.deliveryland.backend.order;

import com.deliveryland.backend.location.Location;
import com.deliveryland.backend.restaurant.Restaurant;
import com.deliveryland.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Customer who placed the order
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // Restaurant the order is from
    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    // Assigned driver (can be null initially)
    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    // Order items
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items;

    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total must be greater than 0")
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_location_id", nullable = false)
    private Location deliveryLocation;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}