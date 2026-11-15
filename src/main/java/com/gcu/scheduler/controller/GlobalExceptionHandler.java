package com.gcu.scheduler.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.gcu.scheduler.exception.AccessDeniedBusinessException;
import com.gcu.scheduler.exception.BookingException;
import com.gcu.scheduler.exception.ResourceNotFoundException;

/**
 * Central error handling: turns exceptions from any controller into a friendly
 * error page and logs them, so users never see a stack trace.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @param ex    the missing-record exception
     * @param model view model
     * @return the error view with 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException ex, Model model) {
        log.warn("Not found: {}", ex.getMessage());
        return errorView(model, 404, ex.getMessage());
    }

    /**
     * @param ex    the ownership exception
     * @param model view model
     * @return the error view with 403
     */
    @ExceptionHandler(AccessDeniedBusinessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(AccessDeniedBusinessException ex, Model model) {
        return errorView(model, 403, ex.getMessage());
    }

    /**
     * @param ex    a business rule exception not handled by the controller
     * @param model view model
     * @return the error view with 400
     */
    @ExceptionHandler(BookingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String businessRule(BookingException ex, Model model) {
        return errorView(model, 400, ex.getMessage());
    }

    /**
     * @param ex    a request for a page or file that does not exist
     * @param model view model
     * @return the error view with 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noPage(NoResourceFoundException ex, Model model) {
        return errorView(model, 404, "That page does not exist.");
    }

    /**
     * @param ex    a URL with a badly formed id or date
     * @param model view model
     * @return the error view with 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badParameter(MethodArgumentTypeMismatchException ex, Model model) {
        return errorView(model, 400, "The link you followed is not valid.");
    }

    /**
     * @param ex    any unexpected exception
     * @param model view model
     * @return the error view with 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        return errorView(model, 500, "Something went wrong. Please try again.");
    }

    private String errorView(Model model, int status, String message) {
        model.addAttribute("status", status);
        model.addAttribute("message", message);
        return "error";
    }
}
