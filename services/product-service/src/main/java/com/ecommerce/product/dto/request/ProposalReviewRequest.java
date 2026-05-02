package com.ecommerce.product.dto.request;

import com.ecommerce.product.entity.ProductProposal.ProposalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class ProposalReviewRequest {

    @NotNull(message = "Karar zorunlu (APPROVED / REJECTED / REVISION_REQUESTED)")
    private ProposalStatus decision;

    private String adminNote;

    private Long existingProductId;

    private String approvedDescription;
}
