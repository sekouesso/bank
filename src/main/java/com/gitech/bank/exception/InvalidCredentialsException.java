package com.gitech.bank.exception;

/**
 * Levée par AuthService.login() aussi bien quand l'utilisateur n'existe pas
 * QUE quand le mot de passe est incorrect. On utilise volontairement le
 * MÊME message dans les deux cas (voir AuthService) pour ne pas révéler à
 * un attaquant si un nom d'utilisateur donné existe ou non ("énumération
 * de comptes").
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
