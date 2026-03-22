package com.deliveryland.backend.common.security;

import lombok.Getter;

@Getter
public enum ApplicationUserPermission {
    // User management
    USER_READ("user:read"),
    USER_WRITE("user:write"),
    USER_DELETE("user:delete"),
    USER_MANAGE_ROLES("user:manage_roles"),

    // Delivery / Order management
    ORDER_READ("order:read"),
    ORDER_CREATE("order:create"),
    ORDER_UPDATE("order:update"),
    ORDER_DELETE("order:delete"),

    // Delivery personnel / Fleet management
    DELIVERY_READ("delivery:read"),
    DELIVERY_UPDATE_STATUS("delivery:update_status"),
    DELIVERY_ASSIGN("delivery:assign"),

    // Notifications / Emails
    NOTIFICATION_SEND("notification:send"),
    NOTIFICATION_READ("notification:read");

    private final String permission;

    ApplicationUserPermission(String permission) {
        this.permission = permission;
    }
}