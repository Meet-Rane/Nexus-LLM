package com.localllm.sovereign_ai_workbench.jwtauth.repository;

import com.localllm.sovereign_ai_workbench.jwtauth.entity.ERole;
import com.localllm.sovereign_ai_workbench.jwtauth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}
