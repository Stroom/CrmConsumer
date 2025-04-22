package com.example.model.queue;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode
public class IdNumberChangeDto {

    private Long id;
    private Long customerId;
    private String idCode;
    private String idCountryCode;
    private LocalDate validFrom;
    private LocalDate validTo;

} 