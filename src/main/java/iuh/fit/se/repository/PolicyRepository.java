package iuh.fit.se.repository;

import iuh.fit.se.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, String> {
    Optional<Policy> findByCodeAndLang(String code, String lang);
}
