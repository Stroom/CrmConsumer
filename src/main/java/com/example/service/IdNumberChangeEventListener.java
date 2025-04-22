package com.example.service;

import com.example.model.IdNumber;
import com.example.model.queue.CustomerChangeEventDto;
import com.example.model.queue.IdNumberChangeDto;
import com.example.repository.IdNumberRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdNumberChangeEventListener {

    private static final String TABLE = "identification_number";
    private static final Set<String> FIELDS = Set.of("id", "customerId", "idCode", "idCountryCode", "validFrom", "validTo");

    private final IdNumberRepository idNumberRepository;

    @RabbitListener(queues = "${queue.crm.update.idNumber.name}")
    @Transactional
    public void handleEvent(CustomerChangeEventDto event) {
        log.info("Received change event: {}", event);
        if (event == null || event.getTable() == null || event.getType() == null || event.getId() == null) {
            log.error("Invalid event received");
            return;
        }

        if (!TABLE.equals(event.getTable())) {
            log.error("Wrong table input: {}", event.getTable());
            return;
        }

        handleChange(event);
    }

    private void handleChange(CustomerChangeEventDto event) {
        var objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        IdNumberChangeDto after = objectMapper.convertValue(event.getAfter(), IdNumberChangeDto.class);
        switch (event.getType()) {
            case "INSERT" -> handleInsert(event.getId(), after);
            case "UPDATE" -> handleUpdate(event.getId(), event.getChangedFields(), after);
            case "DELETE" -> handleDelete(event.getId());
            default -> log.warn("Unknown operation: {}", event.getType());
        }
    }

    private void handleInsert(Long externalId, IdNumberChangeDto after) {
        // TODO external_id should be unique and mandatory in DB to make sure that double-entries would be impossible.
        var existing = idNumberRepository.findByExternalId(externalId);
        if (existing.isEmpty()) {
            IdNumber idNumber = new IdNumber();
            idNumber.setExternalId(externalId);
            idNumber.setCustomerId(after.getCustomerId());
            idNumber.setIdCode(after.getIdCode());
            idNumber.setIdCountryCode(after.getIdCountryCode());
            idNumber.setValidFrom(after.getValidFrom());
            idNumber.setValidTo(after.getValidTo());
            idNumberRepository.save(idNumber);
        }
    }

    private void handleUpdate(Long externalId, Set<String> changedFields, IdNumberChangeDto after) {
        if (changedFields.stream().noneMatch(FIELDS::contains)) {
            return;
        }
        var existing = idNumberRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            var idNumber = existing.get();
            if (changedFields.contains("customerId")) {
                log.error("CustomerId should not change. What now? externalId: {}", externalId);
                idNumber.setCustomerId(after.getCustomerId());
            }
            idNumber.setIdCode(after.getIdCode());
            idNumber.setIdCountryCode(after.getIdCountryCode());
            idNumber.setValidFrom(after.getValidFrom());
            idNumber.setValidTo(after.getValidTo());

            idNumberRepository.save(idNumber);
        } else {
            // TODO maybe add a failsafe that missing data is taken via API? Or do insert based on "before" and changeMap.
            log.warn("Customer does not exist yet. externalId: {}", externalId);
        }
    }

    private void handleDelete(Long externalId) {
        // TODO maybe this would need to cascade? Maybe instead of deleting, use an internal enum value.
        idNumberRepository.deleteByExternalId(externalId);
    }
}