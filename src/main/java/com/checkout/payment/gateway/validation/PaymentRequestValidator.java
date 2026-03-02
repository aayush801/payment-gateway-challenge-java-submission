package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.time.YearMonth;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestValidator {

  private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP");

  public void validate(PostPaymentRequest request) {
    if (request == null) {
      throw new PaymentValidationException("request cannot be null");
    }

    // currency must be one of the supported set
    String currency = request.getCurrency();
    if (currency == null || !SUPPORTED_CURRENCIES.contains(currency)) {
      throw new PaymentValidationException("unsupported currency");
    }

    // expiry must be a valid date in the future
    try {
      YearMonth expiry = YearMonth.of(request.getExpiryYear(), request.getExpiryMonth());
      if (!expiry.isAfter(YearMonth.now())) {
        throw new PaymentValidationException("Payment card expired");
      }
    } 
    catch (PaymentValidationException e) {
      throw e;
    } catch (Exception e) {
      // catch any exception from invalid date construction and rethrow as validation failure
      throw new PaymentValidationException("invalid expiry date", e);
    }
  }
}