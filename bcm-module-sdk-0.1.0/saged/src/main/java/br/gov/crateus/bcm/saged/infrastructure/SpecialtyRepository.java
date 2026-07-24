package br.gov.crateus.bcm.saged.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialtyRepository extends JpaRepository<SpecialtyEntity, UUID> {

    Optional<SpecialtyEntity> findByCode(String code);

    boolean existsByCode(String code);
}
