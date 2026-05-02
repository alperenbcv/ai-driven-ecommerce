package com.ecommerce.product.entity;

import com.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Table(name = "product_proposals", indexes = {
    @Index(name = "idx_proposal_seller", columnList = "seller_id"),
    @Index(name = "idx_proposal_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductProposal extends BaseEntity {

    public enum ProposalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        REVISION_REQUESTED
    }

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 200)
    private String proposedName;

    @Column(columnDefinition = "TEXT")
    private String proposedDescription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal proposedPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.PENDING;

    @Column(length = 500)
    private String adminNote;

    private Long approvedProductId;
}
