package br.com.medicationreminder.domain.repository;

import br.com.medicationreminder.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByWhatsappNumber(String whatsappNumber);
    Optional<User> findByWhatsappNumberAndActiveTrue(String whatsappNumber);
}
