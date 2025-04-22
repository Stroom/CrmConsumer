package com.example.model.queue;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode
public class IdDocumentChangeDto {

    private Long id;
    private Long customerId;
    private String typeCode;
    private String number;
    private String countryCode;
    private LocalDate validFrom;
    private LocalDate validTo;

} 