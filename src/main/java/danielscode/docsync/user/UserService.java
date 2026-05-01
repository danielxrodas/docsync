package danielscode.docsync.user;

import danielscode.docsync.security.JwtTokenProvider;
import danielscode.docsync.security.UserPrincipal;
import danielscode.docsync.shared.exception.ResourceNotFoundException;
import danielscode.docsync.user.dto.AuthResponse;
import danielscode.docsync.user.dto.LoginRequest;
import danielscode.docsync.user.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User saved = userRepository.save(user);
        UserPrincipal principal = UserPrincipal.from(saved);
        String token = jwtTokenProvider.generateToken(principal);
        return new AuthResponse(token, "Bearer", expirationMs, saved.getId(), saved.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = jwtTokenProvider.generateToken(principal);
        return new AuthResponse(token, "Bearer", expirationMs, principal.getId(), principal.getUsername());
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return UserPrincipal.from(user);
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}