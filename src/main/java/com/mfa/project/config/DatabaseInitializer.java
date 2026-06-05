package com.mfa.project.config;
import com.mfa.project.entity.Employee;
import com.mfa.project.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.UUID;
@Configuration
public class DatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String DEFAULT_ADMIN_LOGIN = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final Employee.Role DEFAULT_ADMIN_ROLE = Employee.Role.ADMIN;
    private static final String DEFAULT_ADMIN_QR_SECRET = "ADMIN-DEFAULT-QR-SECRET";
    @Bean
    public CommandLineRunner initializeDefaultAdmin(EmployeeRepository employeeRepository,
                                                    BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            if (employeeRepository.existsByRoleIgnoreCase(DEFAULT_ADMIN_ROLE.name())
                    || employeeRepository.existsByRoleIgnoreCase(DEFAULT_ADMIN_ROLE.authority())) {
                return;
            }
            Employee admin = employeeRepository.findByLogin(DEFAULT_ADMIN_LOGIN).orElseGet(Employee::new);
            admin.setFullName(hasText(admin.getFullName()) ? admin.getFullName() : "Default Administrator");
            admin.setRole(DEFAULT_ADMIN_ROLE);
            admin.setLogin(DEFAULT_ADMIN_LOGIN);
            admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            admin.setActive(true);
            if (!hasText(admin.getQrSecret())) {
                admin.setQrSecret(resolveAdminQrSecret(employeeRepository));
            }
            employeeRepository.save(admin);
            log.info("Default ADMIN account initialized with login '{}'.", DEFAULT_ADMIN_LOGIN);
        };
    }
    private static String resolveAdminQrSecret(EmployeeRepository employeeRepository) {
        return employeeRepository.findByQrSecret(DEFAULT_ADMIN_QR_SECRET)
                .filter(employee -> !DEFAULT_ADMIN_LOGIN.equalsIgnoreCase(employee.getLogin()))
                .map(employee -> "ADMIN-" + UUID.randomUUID())
                .orElse(DEFAULT_ADMIN_QR_SECRET);
    }
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
