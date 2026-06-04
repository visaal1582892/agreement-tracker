package com.medplus.agreement_tracker_backend.security;

import com.medplus.agreement_tracker_backend.entity.User;
import com.medplus.agreement_tracker_backend.entity.UserRole;
import com.medplus.agreement_tracker_backend.repository.UserRepository;
import com.medplus.agreement_tracker_backend.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<String> roles = userRoleRepository.findByUserIdWithRole(user.getId())
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getName().name())
                .toList();

        return UserPrincipal.build(user, roles);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        List<String> roles = userRoleRepository.findByUserIdWithRole(id)
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getName().name())
                .toList();

        return UserPrincipal.build(user, roles);
    }
}
