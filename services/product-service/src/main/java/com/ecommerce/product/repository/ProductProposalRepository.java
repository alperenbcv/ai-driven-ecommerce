package com.ecommerce.product.repository;

import com.ecommerce.product.entity.ProductProposal;
import com.ecommerce.product.entity.ProductProposal.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductProposalRepository extends JpaRepository<ProductProposal, Long> {

    Page<ProductProposal> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    Page<ProductProposal> findByStatusOrderByCreatedAtAsc(ProposalStatus status, Pageable pageable);
}
