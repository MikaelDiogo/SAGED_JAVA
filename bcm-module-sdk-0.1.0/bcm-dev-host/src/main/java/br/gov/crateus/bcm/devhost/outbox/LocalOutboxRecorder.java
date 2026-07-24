package br.gov.crateus.bcm.devhost.outbox;

import br.gov.crateus.bcm.sdk.outbox.OutboxRecorder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LocalOutboxRecorder implements OutboxRecorder {

	private final LocalOutboxEventRepository repository;
	private final ObjectMapper objectMapper;

	public LocalOutboxRecorder(LocalOutboxEventRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void record(String aggregateType, String aggregateId, String eventType, Object payload) {
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Cannot serialize outbox payload for " + eventType, ex);
		}
		repository.save(LocalOutboxEvent.create(aggregateType, aggregateId, eventType, json));
	}
}
