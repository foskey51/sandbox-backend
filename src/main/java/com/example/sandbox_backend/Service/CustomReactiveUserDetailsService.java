package com.example.sandbox_backend.Service;

import com.example.sandbox_backend.Repositories.UserRepository;
import com.example.sandbox_backend.entities.CustomUserDetails;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomReactiveUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    public CustomReactiveUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByEmail(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found")))
                .map(user -> new CustomUserDetails(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPassword(),
                        user.getRoles()
                ));
    }

}
