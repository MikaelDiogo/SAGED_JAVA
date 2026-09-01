package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.UserSpecialtyEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSpecialtyRepository extends JpaRepository<UserSpecialtyEntity, UUID> {
    Page<UserSpecialtyEntity> findByUserIdAndLifecycleStatus(UUID userId, String lifecycleStatus, Pageable pageable);
    Page<UserSpecialtyEntity> findBySpecialtyIdAndLifecycleStatus(UUID specialtyId, String lifecycleStatus, Pageable pageable);
    Page<UserSpecialtyEntity> findAllByLifecycleStatus(String lifecycleStatus, Pageable pageable);
    boolean existsByUserIdAndSpecialtyIdAndLifecycleStatus(UUID userId, UUID specialtyId, String lifecycleStatus);
    void deleteByUserIdAndSpecialtyId(UUID userId, UUID specialtyId);
}
