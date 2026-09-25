package com.kfokam48.attendance.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores {@link ExerciseStatus} under the contract values required by the EXERCICE CHECK constraint. */
@Converter
public class ExerciseStatusConverter implements AttributeConverter<ExerciseStatus, String> {

    @Override
    public String convertToDatabaseColumn(ExerciseStatus status) {
        return status == null ? null : status.code();
    }

    @Override
    public ExerciseStatus convertToEntityAttribute(String code) {
        return code == null ? null : ExerciseStatus.fromCode(code);
    }
}
