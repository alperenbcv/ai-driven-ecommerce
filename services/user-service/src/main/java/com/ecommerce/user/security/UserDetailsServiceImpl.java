package com.ecommerce.user.security;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security'nin kullanıcı yükleme mekanizmasını uygulayan servis sınıfıdır.
 *
 * UserDetailsService interface'i Spring Security tarafından sağlanır.
 * AuthenticationManager login işlemi sırasında kullanıcıyı bulmak için
 * loadUserByUsername metodunu otomatik olarak çağırır.
 *
 * Bu projede username yerine e-posta adresi kullanıldığı için metodun parametresi
 * email olarak değerlendirilir.
 *
 * Akış:
 * 1. Kullanıcı login isteği gönderir.
 * 2. AuthenticationManager bu servisin loadUserByUsername(email) metodunu çağırır.
 * 3. Kullanıcı UserRepository üzerinden aktif kullanıcılar arasından aranır.
 * 4. Kullanıcının rolü Spring Security formatına çevrilir:
 *    USER  → ROLE_USER
 *    ADMIN → ROLE_ADMIN
 * 5. Kullanıcı bilgileri AuthUserPrincipal içine konularak Spring Security'ye döndürülür.
 *
 * AuthUserPrincipal, UserDetails interface'ini implemente eden custom principal sınıfıdır.
 * Bu sayede SecurityContext içinde sadece email değil, userId gibi uygulamaya özel
 * bilgiler de taşınabilir.
 */

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new AuthUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                authorities);
    }
}
