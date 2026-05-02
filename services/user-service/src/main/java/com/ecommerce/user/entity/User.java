package com.ecommerce.user.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * active   → Soft delete — hesabı silmek yerine pasif yapıyoruz.
 *
 * @OneToMany(cascade = ALL, orphanRemoval = true) — Kullanıcı silinirse
 * adresleri de silinir. Listeden çıkarılan adres DB'den de temizlenir.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RoleName role = RoleName.USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /** Satıcıya özgü mağaza bilgileri sadece SELLER rolünde doldurulur. */
    @Column(length = 100)
    private String storeName;

    @Column(length = 500)
    private String storeDescription;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
