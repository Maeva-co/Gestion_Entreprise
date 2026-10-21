package com.entreprise.gestion_entreprise.exception;

/** Violation d'une règle de gestion (renvoyée en 400). */
public class RegleGestionException extends RuntimeException {

    public RegleGestionException(String message) {
        super(message);
    }
}
