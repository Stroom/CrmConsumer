package com.example.service;

import com.example.model.CustomerRelation;
import com.example.model.queue.CustomerChangeEventDto;
import com.example.model.queue.CustomerRelationChangeDto;
import com.example.repository.CustomerRelationRepository;
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
public class CustomerRelationChangeEventListener {

    private static final String TABLE = "customer_relation";
    private static final Set<String> FIELDS = Set.of("id", "customerId", "relatedCustomerId", "typeCode", "validFrom", "validTo");

    private final CustomerRelationRepository customerRelationRepository;

    @RabbitListener(queues = "${queue.crm.update.customerRelation.name}")
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

        CustomerRelationChangeDto after = objectMapper.convertValue(event.getAfter(), CustomerRelationChangeDto.class);
        switch (event.getType()) {
            case "INSERT" -> handleInsert(event.getId(), after);
            case "UPDATE" -> handleUpdate(event.getId(), event.getChangedFields(), after);
            case "DELETE" -> handleDelete(event.getId());
            default -> log.warn("Unknown operation: {}", event.getType());
        }
    }

    private void handleInsert(Long externalId, CustomerRelationChangeDto after) {
        // TODO external_id should be unique and mandatory in DB to make sure that double-entries would be impossible.
        var existing = customerRelationRepository.findByExternalId(externalId);
        if (existing.isEmpty()) {
            CustomerRelation relation = new CustomerRelation();
            relation.setExternalId(externalId);
            relation.setType(after.getTypeCode());
            relation.setCustomerId(after.getCustomerId());
            relation.setRelatedCustomerId(after.getRelatedCustomerId());
            relation.setValidFrom(after.getValidFrom());
            relation.setValidTo(after.getValidTo());
            customerRelationRepository.save(relation);
        }
    }

    private void handleUpdate(Long externalId, Set<String> changedFields, CustomerRelationChangeDto after) {
        if (changedFields.stream().noneMatch(FIELDS::contains)) {
            return;
        }
        var existing = customerRelationRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            var relation = existing.get();
            if (changedFields.contains("customerId")) {
                log.error("CustomerId should not change. What now? externalId: {}", externalId);
                relation.setCustomerId(after.getCustomerId());
            }
            if (changedFields.contains("relatedCustomerId")) {
                log.error("RelatedCustomerId should not change. What now? externalId: {}", externalId);
                relation.setRelatedCustomerId(after.getRelatedCustomerId());
            }
            relation.setType(after.getTypeCode());
            relation.setValidFrom(after.getValidFrom());
            relation.setValidTo(after.getValidTo());

            customerRelationRepository.save(relation);
        } else {
            // TODO maybe add a failsafe that missing data is taken via API? Or do insert based on "before" and changeMap.
            log.warn("Relation does not exist yet. externalId: {}", externalId);
        }
    }

    private void handleDelete(Long externalId) {
        // TODO maybe this would need to cascade? Maybe instead of deleting, use an internal enum value.
        customerRelationRepository.deleteByExternalId(externalId);
    }

}