package br.gov.crateus.bcm.saged.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpecialtyRepository extends JpaRepository<SpecialtyEntity, UUID> {

    Optional<SpecialtyEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SpecialtyEntity s WHERE s.code = :code")
    Optional<SpecialtyEntity> findWithLockByCode(@Param("code") String code);
}
