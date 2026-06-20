package com.concertbooking.concert_booking.payment.controller;

import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.payment.dto.CreatePaymentRequest;
import com.concertbooking.concert_booking.payment.dto.PaymentResponse;
import com.concertbooking.concert_booking.payment.service.PaymentService;
import com.concertbooking.concert_booking.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name="Payments", description = "Razorpay payment integration")
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping("/create-order")
    @Operation(summary = "Create Razorpay payment order for a booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> createOrder(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal User user){
        return ResponseEntity.ok(
                ApiResponse.success(
                        paymentService.createOrder(request,user.getId()),
                        "Payment order created successfully"
                )
        );
    }
    @PostMapping("/webhook")
    @Operation(summary="Razorpay webhook — do not call directly")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature")String signature) {
        paymentService.handleWebhook(payload,signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary ="Get payment status for a booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByBooking(
                                bookingId,user.getId()),"Success"));
    }
    @PostMapping("/simulate-success/{bookingId}")
    @Profile({"dev", "test", "load-test"})
   @Operation( summary = "Simulate successful payment (Testing Only)")
    public ResponseEntity<ApiResponse<Void>> simulateSuccess(@PathVariable UUID bookingId){
        paymentService.simulatePaymentSuccess(bookingId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @Operation(summary = "Cancel a pending payment")
    @PostMapping("/cancel/{bookingId}")
    public ResponseEntity<Void> cancelPayment(@PathVariable UUID bookingId){
        paymentService.cancelPayment(bookingId);
        return ResponseEntity.ok().build();
    }

}
