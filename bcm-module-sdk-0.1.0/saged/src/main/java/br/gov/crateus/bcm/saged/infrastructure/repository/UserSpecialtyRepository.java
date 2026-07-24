package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.UserSpecialtyEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSpecialtyRepository extends JpaRepository<UserSpecialtyEntity, UUID> {
    List<UserSpecialtyEntity> findByUserId(UUID userId);
    List<UserSpecialtyEntity> findBySpecialtyId(UUID specialtyId);
    boolean existsByUserIdAndSpecialtyId(UUID userId, UUID specialtyId);
    void deleteByUserIdAndSpecialtyId(UUID userId, UUID specialtyId);
}
