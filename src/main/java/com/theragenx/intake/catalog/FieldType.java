package com.theragenx.intake.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * The value types a catalog field can have. This is the fixed vocabulary of the catalog;
 * field ids, labels and options are never fixed in code.
 */
public enum FieldType {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    DATE("date"),
    CODED("coded"),
    CODED_MULTI("coded_multi"),
    STRING_LIST("string_list");

    private final String code;

    FieldType(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static FieldType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new CatalogException("Unknown field type in catalog: " + code));
    }

    public boolean hasOptions() {
        return this == CODED || this == CODED_MULTI;
    }
}
