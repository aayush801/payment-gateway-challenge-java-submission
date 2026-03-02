package com.checkout.payment.gateway.controller;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void postValidCardShouldAuthorize() throws Exception {
    String payload = makePayload("4111111111111111", 12, 2030, "USD", 100, "123");
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(1111))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void postValidCardShouldDecline() throws Exception {
    String payload = makePayload("4111111111111112", 12, 2030, "USD", 100, "123");
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()));
  }

  @Test
  void postInvalidRequestMissingCardNumberReturns400() throws Exception {
    String payload = makePayload(null, 12, 2030, "USD", 100, "123");
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postBankErrorReturns502() throws Exception {
    String payload = makePayload("4111111111111110", 12, 2030, "USD", 100, "123");
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(payload))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Acquiring bank unavailable"));
  }

  @Test
  void postCardNumberValidationFails() throws Exception {
    String payload = makePayload("abcd1234", 12, 2030, "USD", 100, "123");
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("card number must be between 14 and 19 digits"));
  }

  @Test
  void postCvvValidationFails() throws Exception {
    String payload = makePayload("4111111111111111", 12, 2030, "USD", 100, "12a");
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("cvv must contain only digits"));
  }

  @Test
  void postAmtValidationFails() throws Exception {
    String payload = makePayload("4111111111111111", 12, 2030, "USD", -100, "123");
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("amount must be positive"));
  }

  @Test
  void postCurrencyValidationFails() throws Exception {
    String payload = makePayload("4111111111111111", 12, 2030, "ABC", 100, "123");
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("unsupported currency"));
  }

  @Test
  void postExpiredCardValidationFails() throws Exception {
    String payload = makePayload("4111111111111111", 1, 2020, "USD", 100, "123");
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType("application/json")
            .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Payment card expired"));
  }
  
  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }

  private String makePayload(String cardNumber,
                             Integer expiryMonth,
                             Integer expiryYear,
                             String currency,
                             Integer amount,
                             String cvv) {
    Map<String, Object> m = new HashMap<>();
    if (cardNumber != null)  m.put("card_number", cardNumber);
    if (expiryMonth != null) m.put("expiry_month", expiryMonth);
    if (expiryYear != null)  m.put("expiry_year", expiryYear);
    if (currency != null)    m.put("currency", currency);
    if (amount != null)      m.put("amount", amount);
    if (cvv != null)         m.put("cvv", cvv);
    try {
      return MAPPER.writeValueAsString(m);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
