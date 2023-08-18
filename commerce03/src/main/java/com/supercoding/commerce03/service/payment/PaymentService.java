package com.supercoding.commerce03.service.payment;

import com.supercoding.commerce03.repository.payment.PaymentDetailRepository;
import com.supercoding.commerce03.repository.payment.PaymentRepository;
import com.supercoding.commerce03.repository.payment.entity.Payment;
import com.supercoding.commerce03.repository.payment.entity.PaymentDetail;
import com.supercoding.commerce03.repository.user.UserRepository;
import com.supercoding.commerce03.repository.user.entity.User;
import com.supercoding.commerce03.service.order.exception.OrderErrorCode;
import com.supercoding.commerce03.service.order.exception.OrderException;
import com.supercoding.commerce03.web.dto.payment.Cancel;
import com.supercoding.commerce03.web.dto.payment.Charge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentDetailRepository paymentDetailRepository;

    public void createPayment(User user){
        Payment payment = Payment.createPayment(user);
        paymentRepository.save(payment);
    }

    @Transactional
    public Charge.Response chargeByCoin(Long userId, Charge.Request request) {
        Payment payment = paymentRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("금액이 부족합니다."));
        int chargeTotalCoin = payment.getTotalCoin() + request.getCoin();
        payment.setTotalCoin(chargeTotalCoin);
        paymentDetailRepository.save(PaymentDetail.builder()
                .payment(payment)
                .createdAt(LocalDateTime.now())
                .businessType(payment.getBusinessType())
                .totalPayCoin(payment.getTotalCoin())
                .payCoin(payment.getCoin())
                .build());
        return Charge.Response.from(payment);
    }

    public List<Charge.Response> findByPaymentId(Long userId) {
        Payment payment = paymentRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("금액이 부족합니다."));
        List<PaymentDetail> paymentDetails = paymentDetailRepository.findAllByPaymentId(payment.getId());
        return paymentDetails.stream()
                .map(paymentDetail -> Charge.Response.builder()
                        .businessType(paymentDetail.getBusinessType())
                        .createdAt(LocalDateTime.now())
                        .coin(paymentDetail.getPayCoin())
                        .totalCoin(paymentDetail.getTotalPayCoin())
                        .build())
                .collect(Collectors.toList());
    }


    public void cancelByBusiness(Long userId, Integer totalAmount) {
        Payment payment = paymentRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("확인이 필요합니다."));
        int chargeTotalCoin = payment.getTotalCoin() + totalAmount;
        payment.setTotalCoin(chargeTotalCoin);
        paymentRepository.save(payment);
    }


    public void orderByBusiness(Long userId, Integer totalAmount) {
        log.info("userId : " + userId);
        Payment payment = paymentRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("확인이 필요합니다."));
        if (payment.getTotalCoin() < totalAmount) {
            throw new OrderException(OrderErrorCode.LACK_OF_POINT);
        } else {
            payment.setTotalCoin(payment.getTotalCoin() - totalAmount);
        }
        paymentRepository.save(payment);

    }

}
