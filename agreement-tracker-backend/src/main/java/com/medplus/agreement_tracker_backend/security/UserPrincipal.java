package com.medplus.agreement_tracker_backend.security;

import com.medplus.agreement_tracker_backend.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean active;
    private final List<String> rights;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String email, String password, boolean active,
                         List<String> rights, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.active = active;
        this.rights = rights;
        this.authorities = authorities;
    }

    public static UserPrincipal build(User user, List<String> roleNames, List<String> rights) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        roleNames.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        rights.forEach(right -> authorities.add(new SimpleGrantedAuthority(right)));

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                List.copyOf(rights),
                authorities
        );
    }

    public boolean hasRight(String rightCode) {
        return rights.contains(rightCode);
    }

    public boolean hasRole(String roleName) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
