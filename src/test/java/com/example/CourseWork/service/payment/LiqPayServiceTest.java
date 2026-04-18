package com.example.CourseWork.service.payment;

import com.example.CourseWork.config.LiqPayConfig;
import com.example.CourseWork.dto.payment.LiqPayCallbackDto;
import com.example.CourseWork.dto.payment.LiqPayCheckoutFormDto;
import com.example.CourseWork.model.Order;
import com.example.CourseWork.service.payment.impl.LiqPayServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.SignatureException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LiqPayServiceTest {

    @Mock
    private LiqPayConfig liqPayConfig;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LiqPayServiceImpl liqPayService;

    @BeforeEach
    void setUp() {
        lenient().when(liqPayConfig.getPublicKey()).thenReturn("test_public_key");
        lenient().when(liqPayConfig.getPrivateKey()).thenReturn("test_private_key");
        lenient().when(liqPayConfig.getPublicBaseUrl()).thenReturn("http://localhost:8080");
        lenient().when(liqPayConfig.getSandbox()).thenReturn(1);
    }

    @Test
    void prepareCheckout_ShouldCalculateCorrectPayableAmount() {
        // Arrange
        Order order = new Order();
        order.setId(123);
        order.setTotalPrice(BigDecimal.valueOf(100.0));
        order.setLoyaltyDiscount(new BigDecimal("10.00"));
        order.setTipAmount(new BigDecimal("5.50"));

        // Payable = 100.0 - 10.0 + 5.5 = 95.50

        // Act
        LiqPayCheckoutFormDto result = liqPayService.prepareCheckout(order);

        // Assert
        assertThat(result.getLiqpayOrderId()).startsWith("order_123_");
        
        // Decode data to verify amount
        String decodedJson = new String(Base64.getDecoder().decode(result.getData()));
        assertThat(decodedJson).contains("\"amount\":95.50");
        assertThat(decodedJson).contains("\"public_key\":\"test_public_key\"");
        assertThat(decodedJson).contains("\"currency\":\"UAH\"");
    }

    @Test
    void prepareCheckout_ShouldHandleDiscountGreaterThanTotal() {
        // Arrange
        Order order = new Order();
        order.setId(456);
        order.setTotalPrice(BigDecimal.valueOf(50.0));
        order.setLoyaltyDiscount(new BigDecimal("60.00")); // Discount > Total
        order.setTipAmount(BigDecimal.ZERO);

        // Payable should be 0.00 (plus tips if any)
        
        // Act
        LiqPayCheckoutFormDto result = liqPayService.prepareCheckout(order);

        // Assert
        String decodedJson = new String(Base64.getDecoder().decode(result.getData()));
        assertThat(decodedJson).contains("\"amount\":0.00");
    }

    @Test
    void validateCallbackSignature_ShouldPassForCorrectSignature() throws SignatureException {
        // Expected signature calculation: Base64(SHA1(privateKey + data + privateKey))
        // privateKey is "test_private_key"
        // input: "test_private_key" + "some_data_from_liqpay" + "test_private_key"
        
        // Let's use the service itself to generate a valid signature for testing
        // This is a bit of a circular dependency in testing, but ensures the logic matches
        LiqPayCheckoutFormDto dummy = liqPayService.prepareCheckout(new Order());
        String validData = dummy.getData();
        String validSignature = dummy.getSignature();

        // Act & Assert
        liqPayService.validateCallbackSignature(validData, validSignature);
    }

    @Test
    void validateCallbackSignature_ShouldThrowExceptionForInvalidSignature() {
        // Arrange
        String data = "tampered_data";
        String invalidSignature = "invalid_signature";

        // Act & Assert
        assertThatThrownBy(() -> liqPayService.validateCallbackSignature(data, invalidSignature))
                .isInstanceOf(SignatureException.class)
                .hasMessage("Invalid LiqPay signature");
    }

    @Test
    void decodeCallbackData_ShouldCorrectlyParseJson() {
        // Arrange
        String json = "{\"order_id\":\"order_123\",\"status\":\"success\",\"amount\":100.0}";
        String encodedData = Base64.getEncoder().encodeToString(json.getBytes());

        // Act
        LiqPayCallbackDto result = liqPayService.decodeCallbackData(encodedData);

        // Assert
        assertThat(result.getOrderId()).isEqualTo("order_123");
        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getAmount()).isEqualTo("100.0");
    }
}
