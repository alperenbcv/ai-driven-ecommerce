package com.ecommerce.user.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Spring Security içinde authenticated kullanıcıyı temsil eden custom principal sınıfıdır.
 *
 * UserDetails interface'i Spring Security'nin standart kullanıcı modelidir.
 * AuthenticationManager ve SecurityContext, kullanıcı bilgisini bu interface üzerinden taşır.
 *
 * Neden custom principal kullandım:
 * Spring Security'nin default User nesnesinde genellikle username, password ve authorities bulunur.
 * Ancak uygulamada sadece e-posta değil, kullanıcının id bilgisine de ihtiyaç duyuyoruz.
 * Bu yüzden AuthUserPrincipal içine id alanı eklendi.
 *
 * Alanlar:
 * - id:
 *   DB'deki kullanıcı ID'sidir. Controller veya service tarafında mevcut kullanıcının
 *   kimliğine erişmek için kullanılabilir.
 *
 * - email:
 *   Kullanıcının login identifier'ıdır. getUsername metodu bunu döndürür.
 *   Spring Security tarafında username olarak email kullanılmış olur.
 *
 * - passwordHash:
 *   BCrypt ile hashlenmiş şifredir. getPassword metodu bunu döndürür.
 *   Authentication sırasında raw password bu hash ile karşılaştırılır.
 *
 * - authorities:
 *   Kullanıcının rollerini/yetkilerini taşır.
 *   Örnek: ROLE_USER, ROLE_SELLER, ROLE_ADMIN
 *
 * Mevcutta kullanıcı hesabı pasif yapma, kilitleme veya şifre süresi doldu gibi kontroller
 * eklemedim ama bu metotlar User entity'sindeki alanlara göre dinamik hale getirilebilir.
 */

@Getter
public class AuthUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthUserPrincipal(Long id, String email, String passwordHash,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
