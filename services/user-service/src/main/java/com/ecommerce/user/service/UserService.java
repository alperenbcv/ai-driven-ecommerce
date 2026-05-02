package com.ecommerce.user.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.request.ChangePasswordRequest;
import com.ecommerce.user.dto.request.SellerProfileRequest;
import com.ecommerce.user.dto.request.UpdateProfileRequest;
import com.ecommerce.user.dto.response.AddressResponse;
import com.ecommerce.user.dto.response.UserResponse;
import com.ecommerce.user.entity.RoleName;

import java.util.List;

public interface UserService {

    PageResponse<UserResponse> getAllUsers(int page, int size);

    UserResponse getCurrentUser(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    List<AddressResponse> getAddresses(Long userId);

    AddressResponse addAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    void setDefaultAddress(Long userId, Long addressId);

    UserResponse changeUserRole(Long targetUserId, RoleName newRole);

    UserResponse updateSellerProfile(Long sellerId, SellerProfileRequest request);
    
    UserResponse updateSellerProfileByAdmin(Long targetUserId, SellerProfileRequest request);
}
