package com.example.model.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerChangeEventDto {
    private String table;
    private String type;
    private Long id;
    Map<String, Object> before;
    Map<String, Object> after;
    Set<String> changedFields;
}