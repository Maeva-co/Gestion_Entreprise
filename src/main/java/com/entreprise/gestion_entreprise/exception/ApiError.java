package com.entreprise.gestion_entreprise.exception;

import java.util.Map;

public record ApiError(String message, Map<String, String> details) {
}
