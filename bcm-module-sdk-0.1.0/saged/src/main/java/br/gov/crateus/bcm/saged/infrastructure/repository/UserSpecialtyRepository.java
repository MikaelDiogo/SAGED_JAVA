package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.UserSpecialtyEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSpecialtyRepository extends JpaRepository<UserSpecialtyEntity, UUID> {
    Page<UserSpecialtyEntity> findByUserId(UUID userId, Pageable pageable);
    Page<UserSpecialtyEntity> findBySpecialtyId(UUID specialtyId, Pageable pageable);
    boolean existsByUserIdAndSpecialtyId(UUID userId, UUID specialtyId);
    void deleteByUserIdAndSpecialtyId(UUID userId, UUID specialtyId);
}
