package com.imdmanuel.book_library.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.imdmanuel.book_library.models.Role;
import com.imdmanuel.book_library.payload.request.ERole;
import com.imdmanuel.book_library.repository.RoleRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        // Create ROLE_USER if it doesn't exist
        if (roleRepository.findByName(ERole.ROLE_USER.name()).isEmpty()) {
            Role userRole = new Role();
            userRole.setName(ERole.ROLE_USER.name());
            roleRepository.save(userRole);
        }

        // Create ROLE_ADMIN if it doesn't exist
        if (roleRepository.findByName(ERole.ROLE_ADMIN.name()).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName(ERole.ROLE_ADMIN.name());
            roleRepository.save(adminRole);
        }
    }
}
