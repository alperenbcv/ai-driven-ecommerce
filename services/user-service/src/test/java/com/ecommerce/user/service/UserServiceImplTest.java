package com.ecommerce.user.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.user.dto.request.ChangePasswordRequest;
import com.ecommerce.user.dto.request.UpdateProfileRequest;
import com.ecommerce.user.dto.response.UserResponse;
import com.ecommerce.user.entity.RoleName;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.AddressRepository;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Testleri")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .firstName("Ali")
                .lastName("Yılmaz")
                .email("ali@test.com")
                .password("$2a$hashedOldPassword")
                .role(RoleName.USER)
                .build();
        activeUser.setActive(true);
    }


    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTests {

        @Test
        @DisplayName("Ad, soyad ve telefon güncelleme → başarılı")
        void updateProfile_success() {
            Long userId = 1L;
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Mehmet");
            request.setLastName("Kaya");
            request.setPhone("05321234567");

            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
            given(userRepository.save(activeUser)).willReturn(activeUser);
            given(userMapper.toUserResponse(activeUser)).willReturn(
                    UserResponse.builder().firstName("Mehmet").lastName("Kaya").build());

            UserResponse result = userService.updateProfile(userId, request);


            assertThat(activeUser.getFirstName()).isEqualTo("Mehmet");
            assertThat(activeUser.getLastName()).isEqualTo("Kaya");
            assertThat(activeUser.getPhone()).isEqualTo("05321234567");

            then(userRepository).should().save(activeUser);
        }

        @Test
        @DisplayName("Kullanıcı bulunamadı → NotFoundException")
        void updateProfile_userNotFound_throwsNotFoundException() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            UpdateProfileRequest badReq = new UpdateProfileRequest();
            badReq.setFirstName("A");
            badReq.setLastName("B");
            assertThatThrownBy(() -> userService.updateProfile(99L, badReq))
                    .isInstanceOf(NotFoundException.class);
        }
    }


    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("Geçerli mevcut şifre + eşleşen yeni şifreler → şifre değişir")
        void changePassword_success() {

            Long userId = 1L;
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("EskiSifre123");
            request.setNewPassword("YeniSifre123!");
            request.setConfirmNewPassword("YeniSifre123!");

            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

            given(passwordEncoder.matches("EskiSifre123", "$2a$hashedOldPassword")).willReturn(true);
            given(passwordEncoder.encode("YeniSifre123!")).willReturn("$2a$hashedNewPassword");
            given(userRepository.save(activeUser)).willReturn(activeUser);

            userService.changePassword(userId, request);


            assertThat(activeUser.getPassword()).isEqualTo("$2a$hashedNewPassword");
            then(userRepository).should().save(activeUser);
        }

        @Test
        @DisplayName("Yeni şifreler eşleşmiyor → BusinessException (DB'ye gitme)")
        void changePassword_newPasswordsMismatch_throwsBusinessException() {

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("EskiSifre123");
            request.setNewPassword("YeniSifre123!");
            request.setConfirmNewPassword("FarklıSifre456!");

            assertThatThrownBy(() -> userService.changePassword(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("eşleşmiyor");

            then(userRepository).should(never()).findById(any());
        }

        @Test
        @DisplayName("Mevcut şifre yanlış → BusinessException")
        void changePassword_wrongCurrentPassword_throwsBusinessException() {

            Long userId = 1L;
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("YanlışEskiSifre");
            request.setNewPassword("YeniSifre123!");
            request.setConfirmNewPassword("YeniSifre123!");

            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

            given(passwordEncoder.matches("YanlışEskiSifre", "$2a$hashedOldPassword")).willReturn(false);


            assertThatThrownBy(() -> userService.changePassword(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Mevcut şifre hatalı");

            then(passwordEncoder).should(never()).encode(any());
        }
    }


    @Nested
    @DisplayName("changeUserRole()")
    class ChangeUserRoleTests {

        @Test
        @DisplayName("USER → SELLER rol değişimi başarılı")
        void changeUserRole_userToSeller_success() {

            Long targetId = 2L;
            activeUser.setRole(RoleName.USER);

            given(userRepository.findById(targetId)).willReturn(Optional.of(activeUser));
            given(userRepository.save(activeUser)).willReturn(activeUser);
            given(userMapper.toUserResponse(activeUser)).willReturn(
                    UserResponse.builder().role(RoleName.SELLER.name()).build());


            UserResponse result = userService.changeUserRole(targetId, RoleName.SELLER);


            assertThat(activeUser.getRole()).isEqualTo(RoleName.SELLER);
            then(userRepository).should().save(activeUser);
        }

        @Test
        @DisplayName("Admin rolündeki kullanıcının rolü indirgenemez → BusinessException")
        void changeUserRole_cannotDemoteAdmin_throwsBusinessException() {

            Long adminId = 5L;
            activeUser.setRole(RoleName.ADMIN);

            given(userRepository.findById(adminId)).willReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> userService.changeUserRole(adminId, RoleName.USER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Admin rolü bu şekilde değiştirilemez");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Pasif kullanıcıya rol değiştirme → NotFoundException")
        void changeUserRole_inactiveUser_throwsNotFoundException() {

            Long userId = 3L;
            activeUser.setActive(false);

            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> userService.changeUserRole(userId, RoleName.SELLER))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Aktif kullanıcı → UserResponse döner")
        void getCurrentUser_success() {
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));
            given(userMapper.toUserResponse(activeUser)).willReturn(
                    UserResponse.builder().email("ali@test.com").build());

            UserResponse result = userService.getCurrentUser(userId);

            assertThat(result.getEmail()).isEqualTo("ali@test.com");
            then(userRepository).should().findById(userId);
        }
    }
}
