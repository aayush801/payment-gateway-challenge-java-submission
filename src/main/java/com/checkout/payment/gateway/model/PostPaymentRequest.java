package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  @NotBlank(message = "card number is required")
  @Size(min = 14, max = 19, message = "card number must be between 14 and 19 digits")
  @Pattern(regexp = "\\d+", message = "card number must contain only digits")
  private String cardNumber;

  @JsonProperty("expiry_month")
  @NotNull(message = "expiry month is required")
  @Min(value = 1, message = "expiry month must be between 1 and 12")
  @Max(value = 12, message = "expiry month must be between 1 and 12")
  private Integer expiryMonth;

  @JsonProperty("expiry_year")
  @NotNull(message = "expiry year is required")
  @Min(value = 1900, message = "expiry year must be a valid year")
  private Integer expiryYear;

  @NotBlank(message = "currency is required")
  @Size(min = 3, max = 3, message = "currency must be a three letter ISO code")
  private String currency;

  @NotNull(message = "amount is required")
  @Min(value = 1, message = "amount must be positive")
  private Integer amount;

  @NotBlank(message = "cvv is required")
  @Size(min = 3, max = 4, message = "cvv must be 3 or 4 digits")
  @Pattern(regexp = "\\d+", message = "cvv must contain only digits")
  private String cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public Integer getCardNumberLastFourDigits() {
    if (cardNumber == null || cardNumber.length() < 4) {
      return 0;
    }
    String lastFourDigits = cardNumber.substring(cardNumber.length() - 4);
    try {
      return Integer.parseInt(lastFourDigits);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  @JsonProperty("expiry_date")
  public String getExpiryDate() {
    if (expiryMonth == null || expiryYear == null) {
      return null;
    }
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

  @Override
  public String toString() {
    String cardNumberLastFour = String.valueOf(getCardNumberLastFourDigits());
    return "PostPaymentRequest{" +
        "cardNumberLastFour=" + cardNumberLastFour +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv=***" +
        '}';
  }
}