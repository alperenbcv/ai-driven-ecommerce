package com.ecommerce.product.dto.response;

import com.ecommerce.product.entity.ProductProposal.ProposalStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductProposalResponse {
    private Long id;
    private Long sellerId;
    private String proposedName;
    private String proposedDescription;
    private BigDecimal proposedPrice;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private ProposalStatus status;
    private String adminNote;
    private Long approvedProductId;
    private LocalDateTime createdAt;
}
