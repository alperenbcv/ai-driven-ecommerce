package com.ecommerce.product.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Kategori entity'si — self-referencing ağaç yapısı.
 *
 * Örnek hiyerarşi:
 *   Elektronik (parent=null)
 *     └─ Telefon (parent=Elektronik)
 *          └─ Akıllı Telefon (parent=Telefon)
 *
 * @ManyToOne parent "Bu kategorinin ebeveyni kim?"
 * @OneToMany children "Bu kategorinin alt kategorileri hangileri?"
 *
 * FetchType.LAZY her ikisi için kategori listelenirken tüm ağacı
 * çekmemek için. Sadece ihtiyaç olduğunda yükle.
 *
 * CascadeType.ALL children için parent silinince alt kategoriler de silinir.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();
}
