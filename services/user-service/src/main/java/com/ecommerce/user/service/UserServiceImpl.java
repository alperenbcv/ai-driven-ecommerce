package com.ecommerce.user.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.request.ChangePasswordRequest;
import com.ecommerce.user.dto.request.SellerProfileRequest;
import com.ecommerce.user.dto.request.UpdateProfileRequest;
import com.ecommerce.user.dto.response.AddressResponse;
import com.ecommerce.user.dto.response.UserResponse;
import com.ecommerce.user.entity.Address;
import com.ecommerce.user.entity.RoleName;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.AddressRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PageResponse.of(users.map(userMapper::toUserResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = findActiveUserById(userId);
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUserById(userId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        User updated = userRepository.save(user);
        log.info("Kullanıcı profili güncellendi: {}", userId);
        return userMapper.toUserResponse(updated);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException("Yeni şifreler eşleşmiyor", "PASSWORD_MISMATCH");
        }
        User user = findActiveUserById(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mevcut şifre hatalı", "INVALID_CURRENT_PASSWORD");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Kullanıcı şifresi değiştirildi: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        findActiveUserById(userId); // kullanıcı var mı kontrol
        return userMapper.toAddressResponseList(addressRepository.findByUserId(userId));
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = findActiveUserById(userId);

        Address address = userMapper.toAddress(request);
        address.setUser(user);

        if (request.isDefaultAddress()) {
            addressRepository.clearDefaultExcept(userId, -1L);
        }

        Address saved = addressRepository.save(address);
        log.info("Kullanıcı {} adres ekledi: {}", userId, saved.getId());
        return userMapper.toAddressResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Adres", addressId));

        address.setTitle(request.getTitle());
        address.setFirstName(request.getFirstName());
        address.setLastName(request.getLastName());
        address.setPhone(request.getPhone());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setFullAddress(request.getFullAddress());

        if (request.isDefaultAddress()) {
            addressRepository.clearDefaultExcept(userId, addressId);
            address.setDefaultAddress(true);
        }

        return userMapper.toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Adres", addressId));

        if (address.isDefaultAddress()) {
            throw new BusinessException("Varsayılan adres silinemez. Önce başka bir adresi varsayılan yapın.");
        }

        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Adres", addressId));

        addressRepository.clearDefaultExcept(userId, addressId);
        address.setDefaultAddress(true);
        addressRepository.save(address);
    }

    /**
     * Admin tarafından kullanıcı rolü değiştirme.
     * Admin kendi rolünü değiştirmeye çalışırsa hata ver kendini kilitlememesi için.
     */
    @Override
    @Transactional
    public UserResponse changeUserRole(Long targetUserId, RoleName newRole) {
        User target = findActiveUserById(targetUserId);

        if (target.getRole() == RoleName.ADMIN && newRole != RoleName.ADMIN) {
            throw new BusinessException("Admin rolü bu şekilde değiştirilemez. Doğrudan DB üzerinden işlem yapın.");
        }

        target.setRole(newRole);
        User saved = userRepository.save(target);
        log.info("Kullanıcı rolü değiştirildi: userId={}, yeniRol={}", targetUserId, newRole);
        return userMapper.toUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateSellerProfile(Long sellerId, SellerProfileRequest request) {
        User user = findActiveUserById(sellerId);
        if (user.getRole() != RoleName.SELLER && user.getRole() != RoleName.ADMIN) {
            throw new BusinessException("Mağaza profili yalnızca satıcı hesaplarında güncellenebilir");
        }
        user.setStoreName(request.getStoreName());
        user.setStoreDescription(request.getStoreDescription());
        log.info("Satıcı mağaza profili güncellendi: userId={}, storeName={}", sellerId, request.getStoreName());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateSellerProfileByAdmin(Long targetUserId, SellerProfileRequest request) {
        User user = findActiveUserById(targetUserId);
        user.setStoreName(request.getStoreName());
        user.setStoreDescription(request.getStoreDescription());
        log.info("Admin mağaza profili güncelledi: userId={}, storeName={}", targetUserId, request.getStoreName());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    private User findActiveUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("Kullanıcı", userId));
    }
}
