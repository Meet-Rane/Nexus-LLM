package com.localllm.sovereign_ai_workbench.jwtauth.controller;

import com.localllm.sovereign_ai_workbench.jwtauth.entity.ERole;
import com.localllm.sovereign_ai_workbench.jwtauth.entity.Role;
import com.localllm.sovereign_ai_workbench.jwtauth.entity.User;
import com.localllm.sovereign_ai_workbench.jwtauth.payload.request.LoginRequest;
import com.localllm.sovereign_ai_workbench.jwtauth.payload.request.SignupRequest;
import com.localllm.sovereign_ai_workbench.jwtauth.payload.response.JwtResponse;
import com.localllm.sovereign_ai_workbench.jwtauth.payload.response.MessageResponse;
import com.localllm.sovereign_ai_workbench.jwtauth.repository.RoleRepository;
import com.localllm.sovereign_ai_workbench.jwtauth.repository.UserRepository;
import com.localllm.sovereign_ai_workbench.jwtauth.security.jwt.JwtUtils;
import com.localllm.sovereign_ai_workbench.jwtauth.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthController(
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder encoder,
        JwtUtils jwtUtils
    ){
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<Role> roles = new HashSet<>();
        boolean firstAccount = userRepository.count() == 0;
        ERole assignedRole = firstAccount ? ERole.ROLE_ADMIN : ERole.ROLE_USER;
        roles.add(roleRepository.findByName(assignedRole)
                .orElseThrow(() -> new RuntimeException("Error: Required role is not initialized.")));

        user.setRoles(roles);
        userRepository.save(user);

        String message = firstAccount
                ? "Administrator account created successfully."
                : "Operator account created successfully.";
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return ResponseEntity.status(401).body(new MessageResponse("Authentication required."));
        }
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();
        return ResponseEntity.ok(Map.of(
                "id", userDetails.getId(),
                "username", userDetails.getUsername(),
                "email", userDetails.getEmail(),
                "roles", roles
        ));
    }
}
