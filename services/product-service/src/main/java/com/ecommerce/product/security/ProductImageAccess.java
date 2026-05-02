package com.ecommerce.product.security;

import com.ecommerce.product.entity.Product;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Katalog ürün görselleri: yalnızca ADMIN veya ürünün {@code sellerId} alanına eşleşen SELLER.
 */
@Component
public class ProductImageAccess {

    public void checkCanManage(Product product) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Oturum gerekli");
        }
        if (hasRole(auth, "ROLE_ADMIN")) {
            return;
        }
        if (!hasRole(auth, "ROLE_SELLER")) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok");
        }
        Long ownerId = product.getSellerId();
        if (ownerId == null) {
            throw new AccessDeniedException("Bu katalog ürününün görsellerini yalnızca yönetici güncelleyebilir");
        }
        long callerId = Long.parseLong(auth.getPrincipal().toString());
        if (ownerId.longValue() != callerId) {
            throw new AccessDeniedException("Yalnızca ürün sahibi satıcı veya yönetici görsel yönetebilir");
        }
    }

    private static boolean hasRole(Authentication auth, String role) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (role.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
