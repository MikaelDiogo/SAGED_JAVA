package br.gov.crateus.bcm.devhost.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "outbox_event", schema = "sdk")
public class LocalOutboxEvent {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "aggregate_type", nullable = false, length = 128)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false, length = 64)
	private String aggregateId;

	@Column(name = "event_type", nullable = false, length = 128)
	private String eventType;

	@Column(name = "payload", nullable = false, columnDefinition = "TEXT")
	private String payload;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	public static LocalOutboxEvent create(
			String aggregateType,
			String aggregateId,
			String eventType,
			String payload
	) {
		LocalOutboxEvent event = new LocalOutboxEvent();
		event.id = UUID.randomUUID();
		event.aggregateType = aggregateType;
		event.aggregateId = aggregateId;
		event.eventType = eventType;
		event.payload = payload;
		event.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
		return event;
	}
}
