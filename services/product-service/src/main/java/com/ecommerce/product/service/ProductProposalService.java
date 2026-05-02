package com.ecommerce.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.product.dto.request.ProductProposalRequest;
import com.ecommerce.product.dto.request.ProposalReviewRequest;
import com.ecommerce.product.dto.response.ProductProposalResponse;
import com.ecommerce.product.entity.ProductProposal.ProposalStatus;

public interface ProductProposalService {
    ProductProposalResponse submitProposal(Long sellerId, ProductProposalRequest request);
    ProductProposalResponse updateProposal(Long proposalId, Long sellerId, ProductProposalRequest request);
    ProductProposalResponse reviewProposal(Long proposalId, ProposalReviewRequest request);
    PageResponse<ProductProposalResponse> getSellerProposals(Long sellerId, int page, int size);
    PageResponse<ProductProposalResponse> getPendingProposals(int page, int size);
}
