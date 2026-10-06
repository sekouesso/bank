package com.gitech.bank.exception;

/** Violation d'une règle métier (pas juste une donnée mal formée) → mappée en 422. */
public class SoldeInsuffisantException extends RuntimeException {
    public SoldeInsuffisantException(String message) {
        super(message);
    }
}
