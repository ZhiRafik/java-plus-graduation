package ru.practicum.exception;

public class ConflictPropertyConstraintException extends RuntimeException {
    public ConflictPropertyConstraintException(String message) {
        super(message);
    }
}
