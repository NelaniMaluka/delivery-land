package com.deliveryland.backend.common.security;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

import static com.deliveryland.backend.common.security.ApplicationUserPermission.*;

@Getter
public enum ApplicationUserRole {

    CUSTOMER(Set.of(
            USER_READ,          // read own info
            USER_WRITE,         // update own info
            ORDER_READ,         // view orders
            ORDER_CREATE,       // place orders
            NOTIFICATION_READ   // see notifications
    )),

    DRIVER(Set.of(
            USER_READ,              // read own info
            DELIVERY_READ,          // view assigned deliveries
            DELIVERY_UPDATE_STATUS, // update delivery status
            NOTIFICATION_READ       // see notifications
    )),

    STORE_OWNER(Set.of(
            USER_READ,        // read own info
            ORDER_READ,       // see store orders
            ORDER_UPDATE,     // update order status
            ORDER_DELETE,     // cancel orders
            DELIVERY_READ,    // view delivery status
            DELIVERY_ASSIGN,  // assign deliveries to drivers
            NOTIFICATION_SEND,// send notifications
            NOTIFICATION_READ // read notifications
    )),

    ADMIN(Set.of(
            USER_READ,
            USER_WRITE,
            USER_DELETE,
            USER_MANAGE_ROLES,
            ORDER_READ,
            ORDER_CREATE,
            ORDER_UPDATE,
            ORDER_DELETE,
            DELIVERY_READ,
            DELIVERY_UPDATE_STATUS,
            DELIVERY_ASSIGN,
            NOTIFICATION_SEND,
            NOTIFICATION_READ
    ));

    private final Set<ApplicationUserPermission> permissions;

    ApplicationUserRole(Set<ApplicationUserPermission> permissions) {
        this.permissions = permissions;
    }

    public Set<SimpleGrantedAuthority> grantedAuthorities() {
        Set<SimpleGrantedAuthority> perms = getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.getPermission()))
                .collect(Collectors.toSet());
        perms.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return perms;
    }
}