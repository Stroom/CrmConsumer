package com.example.service;

import com.example.model.IdDocument;
import com.example.model.queue.CustomerChangeEventDto;
import com.example.model.queue.IdDocumentChangeDto;
import com.example.repository.IdDocumentRepository;
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
public class IdDocumentChangeEventListener {

    private static final String TABLE = "identification_document";
    private static final Set<String> FIELDS = Set.of("id", "customerId", "typeCode", "number", "countryCode", "validFrom", "validTo");

    private final IdDocumentRepository idDocumentRepository;

    @RabbitListener(queues = "${queue.crm.update.idDocument.name}")
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

        IdDocumentChangeDto after = objectMapper.convertValue(event.getAfter(), IdDocumentChangeDto.class);
        switch (event.getType()) {
            case "INSERT" -> handleInsert(event.getId(), after);
            case "UPDATE" -> handleUpdate(event.getId(), event.getChangedFields(), after);
            case "DELETE" -> handleDelete(event.getId());
            default -> log.warn("Unknown operation: {}", event.getType());
        }
    }

    private void handleInsert(Long externalId, IdDocumentChangeDto after) {
        // TODO external_id should be unique and mandatory in DB to make sure that double-entries would be impossible.
        var existing = idDocumentRepository.findByExternalId(externalId);
        if (existing.isEmpty()) {
            IdDocument document = new IdDocument();
            document.setExternalId(externalId);
            document.setCustomerId(after.getCustomerId());
            document.setType(after.getTypeCode());
            document.setNumber(after.getNumber());
            document.setCountryCode(after.getCountryCode());
            document.setValidFrom(after.getValidFrom());
            document.setValidTo(after.getValidTo());
            idDocumentRepository.save(document);
        }
    }

    private void handleUpdate(Long externalId, Set<String> changedFields, IdDocumentChangeDto after) {
        if (changedFields.stream().noneMatch(FIELDS::contains)) {
            return;
        }
        var existing = idDocumentRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            var document = existing.get();
            if (changedFields.contains("customerId")) {
                log.error("CustomerId should not change. What now? externalId: {}", externalId);
                document.setCustomerId(after.getCustomerId());
            }
            document.setType(after.getTypeCode());
            document.setNumber(after.getNumber());
            document.setCountryCode(after.getCountryCode());
            document.setValidFrom(after.getValidFrom());
            document.setValidTo(after.getValidTo());

            idDocumentRepository.save(document);
        } else {
            // TODO maybe add a failsafe that missing data is taken via API? Or do insert based on "before" and changeMap.
            log.warn("Customer does not exist yet. externalId: {}", externalId);
        }
    }

    private void handleDelete(Long externalId) {
        // TODO maybe this would need to cascade? Maybe instead of deleting, use an internal enum value.
        idDocumentRepository.deleteByExternalId(externalId);
    }

}