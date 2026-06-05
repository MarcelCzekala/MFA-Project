package com.mfa.project.repository;
import com.mfa.project.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByFingerprintId(String fingerprintId);
    Optional<Employee> findByNfcUid(String nfcUid);
    Optional<Employee> findByFingerprintIdAndNfcUid(String fingerprintId, String nfcUid);
    Optional<Employee> findByQrSecret(String qrSecret);
    boolean existsByNfcUid(String nfcUid);
    boolean existsByRoleIgnoreCase(String role);
    @Query("SELECT MAX(CAST(e.fingerprintId AS int)) FROM Employee e")
    Integer findMaxFingerprintId();
    Optional<Employee> findByLogin(String login);
}
