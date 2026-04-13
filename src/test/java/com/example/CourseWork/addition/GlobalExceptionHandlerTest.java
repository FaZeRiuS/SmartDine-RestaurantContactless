package com.example.CourseWork.addition;

import com.example.CourseWork.exception.NotFoundException;
import com.example.CourseWork.exception.InsufficientPointsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SignatureException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/insufficient-points")
        public void throwInsufficientPoints() {
            throw new InsufficientPointsException("Test message");
        }

        @GetMapping("/test/access-denied")
        public void throwAccessDenied() {
            throw new AccessDeniedException("Access denied");
        }

        @GetMapping("/test/signature-error")
        public void throwSignature() throws SignatureException {
            throw new SignatureException("Invalid sig");
        }

        @GetMapping("/test/runtime-not-found")
        public void throwNotFound() {
            throw new NotFoundException("Item not found");
        }

        @GetMapping("/test/generic-error")
        public void throwGeneric() throws Exception {
            throw new Exception("Unexpected");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleInsufficientPoints_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/test/insufficient-points"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void handleAccessDenied_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void handleSignatureException_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/test/signature-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void handleRuntimeException_ShouldMapToCorrectStatus() throws Exception {
        mockMvc.perform(get("/test/runtime-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists());
    }
}
