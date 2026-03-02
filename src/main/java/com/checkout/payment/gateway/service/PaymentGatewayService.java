package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.BankResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;
  private final PaymentRequestValidator validator;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      BankClient bankClient,
      PaymentRequestValidator validator) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
    this.validator = validator;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.debug("Processing payment request {}", paymentRequest);

    // validate request details that are beyond sanity checks
    validator.validate(paymentRequest);

    // submit to acquiring bank
    BankResponse bankResp;
    try {
      bankResp = bankClient.submitPayment(paymentRequest);
    } catch (BankCommunicationException e) {
      throw e;
    }
    PaymentStatus status = bankResp.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;

    // build response
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(status);
    response.setCardNumberLastFour(paymentRequest.getCardNumberLastFourDigits());
    response.setExpiryMonth(paymentRequest.getExpiryMonth());
    response.setExpiryYear(paymentRequest.getExpiryYear());
    response.setCurrency(paymentRequest.getCurrency());
    response.setAmount(paymentRequest.getAmount());

    // persist
    paymentsRepository.add(response);

    LOG.debug("Stored payment {} with status {}", response.getId(), status);
    return response;
  }
}
