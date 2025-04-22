package com.example.model.queue;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode
public class CustomerRelationChangeDto {

    private Long id;
    private Long customerId;
    private Long relatedCustomerId;
    private String typeCode;
    private LocalDate validFrom;
    private LocalDate validTo;

} 