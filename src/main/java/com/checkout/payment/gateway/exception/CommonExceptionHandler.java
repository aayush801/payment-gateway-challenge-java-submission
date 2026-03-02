package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Exception happened", ex);
    return new ResponseEntity<>(new ErrorResponse("Page not found"),
        HttpStatus.NOT_FOUND);
  }
  
  @ExceptionHandler(BankCommunicationException.class)
  public ResponseEntity<ErrorResponse> handleBankError(
      BankCommunicationException ex) {
    LOG.error("Bank communication failure", ex);
    return new ResponseEntity<>(new ErrorResponse("Acquiring bank unavailable"),
        HttpStatus.BAD_GATEWAY);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
    LOG.error("Input Validation failed: ", ex);
    String message = ex.getBindingResult()
      .getFieldErrors()
      .stream()
      .map(error -> error.getDefaultMessage())
      .findFirst()
      .orElse("Invalid request");
    return new ResponseEntity<>(new ErrorResponse(message), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(PaymentValidationException.class)
  public ResponseEntity<ErrorResponse> handlePaymentValidation(PaymentValidationException ex) {
    LOG.error("Payment validation failed: ", ex);
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), HttpStatus.BAD_REQUEST);
  }
}
