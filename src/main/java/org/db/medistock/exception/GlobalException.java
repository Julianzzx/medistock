package org.db.medistock.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalException {
    private static final Logger logger = LoggerFactory.getLogger(GlobalException.class);

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        logger.error("Excepción durante el procesamiento de una solicitud", ex);
        String mensaje = ex.getMessage() != null ? ex.getMessage() : "Ocurrió un error inesperado en el sistema.";
        model.addAttribute("error", mensaje);
        model.addAttribute("errorType", ex.getClass().getSimpleName());
        return "Exception";
    }
}
