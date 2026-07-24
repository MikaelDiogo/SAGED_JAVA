package br.gov.crateus.bcm.devhost.outbox;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalOutboxEventRepository extends JpaRepository<LocalOutboxEvent, UUID> {
}
