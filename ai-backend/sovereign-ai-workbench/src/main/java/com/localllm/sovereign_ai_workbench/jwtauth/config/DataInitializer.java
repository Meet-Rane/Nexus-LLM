package com.localllm.sovereign_ai_workbench.jwtauth.config;

import com.localllm.sovereign_ai_workbench.jwtauth.entity.ERole;
import com.localllm.sovereign_ai_workbench.jwtauth.entity.Role;
import com.localllm.sovereign_ai_workbench.jwtauth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the roles table with ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN on startup
 * if they don't already exist. Without this, signup will fail because
 * AuthController looks up roles that must already exist in the DB.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        for (ERole role : ERole.values()) {
            roleRepository.findByName(role).orElseGet(() -> roleRepository.save(new Role(role)));
        }
    }
}
