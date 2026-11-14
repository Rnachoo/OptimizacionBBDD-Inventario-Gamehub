package com.inventory.controllers;

import com.inventory.exceptions.InventoryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    // Reglas de negocio (stock insuficiente, inventario inexistente, etc.)
    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<Map<String, String>> handleInventoryException(InventoryException ex) {
        String mensaje = ex.getMessage() == null ? "Error de inventario" : ex.getMessage();
        HttpStatus status = mensaje.toLowerCase().contains("no encontrado")
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("mensaje", mensaje));
    }
}