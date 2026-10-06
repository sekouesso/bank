package com.gitech.bank.exception;

/** Ex : nom d'utilisateur ou numéro de compte déjà existant. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
