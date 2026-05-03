package com.ecommerce.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.product.messaging.ProductEventPublisher;
import com.ecommerce.product.dto.request.ProductProposalRequest;
import com.ecommerce.product.dto.request.ProposalReviewRequest;
import com.ecommerce.product.dto.response.ProductProposalResponse;
import com.ecommerce.product.entity.*;
import com.ecommerce.product.entity.ProductProposal.ProposalStatus;
import com.ecommerce.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductProposalServiceImpl implements ProductProposalService {

    private final ProductProposalRepository proposalRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductEventPublisher productEventPublisher;

    @Override
    @Transactional
    public ProductProposalResponse submitProposal(Long sellerId, ProductProposalRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadı"));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new NotFoundException("Marka bulunamadı"));
        }

        ProductProposal proposal = ProductProposal.builder()
                .sellerId(sellerId)
                .proposedName(request.getProposedName())
                .proposedDescription(request.getProposedDescription())
                .proposedPrice(request.getProposedPrice())
                .category(category)
                .brand(brand)
                .build();

        ProductProposal saved = proposalRepository.save(proposal);
        log.info("Ürün teklifi oluşturuldu: sellerId={}, proposalId={}", sellerId, saved.getId());
        return toResponse(saved);
    }

    /**
     * Seller, REVISION_REQUESTED olan teklifini düzenleyerek yeniden gönderir.
     * Durum PENDING'e geri alınır, adminNote temizlenir.
     */
    @Override
    @Transactional
    public ProductProposalResponse updateProposal(Long proposalId, Long sellerId, ProductProposalRequest request) {
        ProductProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new NotFoundException("Teklif bulunamadı"));

        if (!proposal.getSellerId().equals(sellerId)) {
            throw new BusinessException("Bu teklif size ait değil");
        }
        if (proposal.getStatus() != ProposalStatus.REVISION_REQUESTED) {
            throw new BusinessException("Sadece revizyon istenen teklifler güncellenebilir (mevcut: " + proposal.getStatus() + ")");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadı"));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new NotFoundException("Marka bulunamadı"));
        }

        proposal.setProposedName(request.getProposedName());
        proposal.setProposedDescription(request.getProposedDescription());
        proposal.setProposedPrice(request.getProposedPrice());
        proposal.setCategory(category);
        proposal.setBrand(brand);
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setAdminNote(null);

        log.info("Teklif revize edildi ve yeniden gönderildi: proposalId={}, sellerId={}", proposalId, sellerId);
        return toResponse(proposalRepository.save(proposal));
    }

    @Override
    @Transactional
    public ProductProposalResponse reviewProposal(Long proposalId, ProposalReviewRequest request) {
        ProductProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new NotFoundException("Teklif bulunamadı"));

        if (proposal.getStatus() != ProposalStatus.PENDING
                && proposal.getStatus() != ProposalStatus.REVISION_REQUESTED) {
            throw new BusinessException("Bu teklif zaten işlem görmüş: " + proposal.getStatus());
        }

        proposal.setStatus(request.getDecision());
        proposal.setAdminNote(request.getAdminNote());

        if (request.getDecision() == ProposalStatus.APPROVED) {
            Long approvedProductId;

            Product approvedProduct;
            if (request.getExistingProductId() != null) {
                // Bu kısım önemli aynı ürün adıyla çokça ürün oluşmasın diye mevcut ürüne bağlama yapıyoruz.
                approvedProduct = productRepository.findById(request.getExistingProductId())
                        .orElseThrow(() -> new NotFoundException("Bağlanmak istenen ürün bulunamadı: " + request.getExistingProductId()));
                approvedProductId = approvedProduct.getId();
                log.info("Teklif mevcut ürüne bağlandı: proposalId={}, productId={}", proposalId, approvedProductId);
            } else {
                String approvedDescription = request.getApprovedDescription() != null
                        && !request.getApprovedDescription().isBlank()
                        ? request.getApprovedDescription().trim()
                        : proposal.getProposedDescription();

                Product product = Product.builder()
                        .name(proposal.getProposedName())
                        .description(approvedDescription)
                        .price(proposal.getProposedPrice())
                        .category(proposal.getCategory())
                        .brand(proposal.getBrand())
                        .sellerId(proposal.getSellerId())
                        .build();
                approvedProduct = productRepository.save(product);
                approvedProductId = approvedProduct.getId();
                log.info("Teklif onaylandı ve yeni ürün oluşturuldu: proposalId={}, productId={}", proposalId, approvedProductId);
            }

            if (request.getExistingProductId() == null) {
                productEventPublisher.publishCreated(approvedProduct);
            }

            proposal.setApprovedProductId(approvedProductId);
        } else {
            log.info("Teklif güncellendi: proposalId={}, durum={}", proposalId, request.getDecision());
        }

        return toResponse(proposalRepository.save(proposal));
    }

    @Override
    public PageResponse<ProductProposalResponse> getSellerProposals(Long sellerId, int page, int size) {
        Page<ProductProposal> proposals = proposalRepository.findBySellerIdOrderByCreatedAtDesc(
                sellerId, PageRequest.of(page, size));
        return PageResponse.of(proposals.map(this::toResponse));
    }

    @Override
    public PageResponse<ProductProposalResponse> getPendingProposals(int page, int size) {
        Page<ProductProposal> proposals = proposalRepository.findByStatusOrderByCreatedAtAsc(
                ProposalStatus.PENDING,
                PageRequest.of(page, size, Sort.by("createdAt").ascending()));
        return PageResponse.of(proposals.map(this::toResponse));
    }

    private ProductProposalResponse toResponse(ProductProposal p) {
        return ProductProposalResponse.builder()
                .id(p.getId())
                .sellerId(p.getSellerId())
                .proposedName(p.getProposedName())
                .proposedDescription(p.getProposedDescription())
                .proposedPrice(p.getProposedPrice())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .brandId(p.getBrand() != null ? p.getBrand().getId() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .status(p.getStatus())
                .adminNote(p.getAdminNote())
                .approvedProductId(p.getApprovedProductId())
                .createdAt(p.getCreatedAt())
                .build();
    }

}
