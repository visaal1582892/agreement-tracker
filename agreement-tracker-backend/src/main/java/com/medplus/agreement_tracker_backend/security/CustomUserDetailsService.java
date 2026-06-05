package com.medplus.agreement_tracker_backend.security;

import com.medplus.agreement_tracker_backend.entity.User;
import com.medplus.agreement_tracker_backend.repository.UserRepository;
import com.medplus.agreement_tracker_backend.repository.UserRoleRepository;
import com.medplus.agreement_tracker_backend.service.UserRightsService;
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
    private final UserRightsService userRightsService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return buildPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        return buildPrincipal(user);
    }

    private UserPrincipal buildPrincipal(User user) {
        List<String> roles = userRoleRepository.findByUserIdWithRole(user.getId()).stream()
                .map(ur -> ur.getRole().getName().name())
                .toList();
        List<String> rights = userRightsService.resolveRightsForUser(user.getId());
        return UserPrincipal.build(user, roles, rights);
    }
}
