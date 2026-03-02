package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import com.checkout.payment.gateway.client.BankResponse;

@Component
public class BankClient {

  private final RestTemplate restTemplate;
  private static final String ENDPOINT = "http://localhost:8080/payments";

  public BankClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public BankResponse submitPayment(PostPaymentRequest request) {
    try {
      return restTemplate.postForObject(ENDPOINT, request, BankResponse.class);
    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode().is5xxServerError()) {
        throw new BankCommunicationException("acquiring bank returned error", ex);
      }
      BankResponse resp = new BankResponse();
      resp.setAuthorized(false);
      return resp;
    }
  }
}