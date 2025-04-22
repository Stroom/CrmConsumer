package com.example.service;

import com.example.model.Customer;
import com.example.model.queue.CustomerChangeDto;
import com.example.model.queue.CustomerChangeEventDto;
import com.example.repository.CustomerRepository;
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
public class CustomerLVChangeEventListener {

    private static final String TABLE = "customer";
    private static final Set<String> FIELDS = Set.of("id", "fullName", "firstName", "middleName", "lastName", "dateOfBirth", "orgCode");

    private final CustomerRepository customerRepository;

    @RabbitListener(queues = "${queue.crm.update.customer.name}")
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

        CustomerChangeDto after = objectMapper.convertValue(event.getAfter(), CustomerChangeDto.class); // TODO Use Dto here
        switch (event.getType()) {
            case "INSERT" -> handleInsert(event.getId(), after);
            case "UPDATE" -> handleUpdate(event.getId(), event.getChangedFields(), after);
            case "DELETE" -> handleDelete(event.getId());
            default -> log.warn("Unknown operation: {}", event.getType());
        }
    }

    private void handleInsert(Long externalId, CustomerChangeDto after) {
        // TODO external_id should be unique and mandatory in DB to make sure that double-entries would be impossible.
        var existing = customerRepository.findByExternalId(externalId);
        if (existing.isEmpty()) {
            Customer customer = new Customer();
            customer.setExternalId(externalId);
            customer.setFullName(after.getFullName());
            customer.setFirstName(after.getFirstName());
            customer.setMiddleName(after.getMiddleName());
            customer.setLastName(after.getLastName());
            customer.setDateOfBirth(after.getDateOfBirth());
            customer.setOrgCode(after.getOrgCode());
            customerRepository.save(customer);
        }
    }

    private void handleUpdate(Long externalId, Set<String> changedFields, CustomerChangeDto after) {
        if (changedFields.stream().noneMatch(FIELDS::contains)) {
            return;
        }
        var existing = customerRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            var customer = existing.get();
            customer.setFullName(after.getFullName());
            customer.setFirstName(after.getFirstName());
            customer.setMiddleName(after.getMiddleName());
            customer.setLastName(after.getLastName());
            customer.setDateOfBirth(after.getDateOfBirth());
            // TODO maybe LV registry might just throw an error for this change or delete the customer if it is not LVBIG?
            // TODO alternatively if LV is changed to something else... maybe it would make sense to listen to all changes instead.
            if (changedFields.contains("orgCode")) {
                log.error("Customer id: {} orgCode changed.", externalId);
                customer.setOrgCode(after.getOrgCode());
            }

            customerRepository.save(customer);
        } else {
            // TODO maybe add a failsafe that missing data is taken via API? Or do insert based on "before" and changeMap.
            log.warn("Customer does not exist yet. externalId: {}", externalId);
        }
    }

    private void handleDelete(Long externalId) {
        // TODO maybe this would need to cascade? Maybe instead of deleting, use an internal enum value.
        customerRepository.deleteByExternalId(externalId);
    }

}