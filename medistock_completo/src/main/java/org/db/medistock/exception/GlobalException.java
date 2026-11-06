package org.db.medistock.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalException {

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        String mensaje = ex.getMessage() != null ? ex.getMessage() : "Ocurrió un error inesperado en el sistema.";
        model.addAttribute("error", mensaje);
        return "Exception";
    }
}