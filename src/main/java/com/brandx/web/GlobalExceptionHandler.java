package com.brandx.web;

import com.brandx.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Turns service-layer exceptions into rendered pages with the right status code.
 *
 * <p>Note {@link com.brandx.service.EntityInUseException} is deliberately absent: a
 * refused delete belongs back on the list screen as a flash message, which the
 * controllers handle, not on a dedicated error page.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "not-found";
    }
}
