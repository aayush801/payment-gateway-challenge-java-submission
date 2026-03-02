package com.checkout.payment.gateway.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentRequestValidatorTest {

  private PaymentRequestValidator validator;

  @BeforeEach
  void setup() {
    validator = new PaymentRequestValidator();
  }

  private PostPaymentRequest baseRequest() {
    PostPaymentRequest r = new PostPaymentRequest();
    r.setCardNumber("4111111111111111");
    r.setExpiryMonth(12);
    r.setExpiryYear(2030);
    r.setCurrency("USD");
    r.setAmount(100);
    r.setCvv("123");
    return r;
  }

  @Test
  void validRequestDoesNotThrow() {
    validator.validate(baseRequest());
  }

  @Test
  void unsupportedCurrencyThrows() {
    PostPaymentRequest r = baseRequest();
    r.setCurrency("AAA");
    assertThrows(PaymentValidationException.class, () -> validator.validate(r));
  }

  @Test
  void expiredCardThrows() {
    PostPaymentRequest r = baseRequest();
    r.setExpiryYear(2020);
    r.setExpiryMonth(1);
    assertThrows(PaymentValidationException.class, () -> validator.validate(r));
  }
}