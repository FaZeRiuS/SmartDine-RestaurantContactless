package com.example.CourseWork.service.qr.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    @InjectMocks
    private QrCodeServiceImpl qrCodeService;

    @Test
    void generateQrCode_ShouldReturnBytes_WhenValidText() throws Exception {
        // Arrange
        String text = "https://smartdine.com/menu?table=5";
        int width = 250;
        int height = 250;

        // Act
        byte[] qrCode = qrCodeService.generateQrCode(text, width, height);

        // Assert
        assertNotNull(qrCode);
        assertTrue(qrCode.length > 0);
        
        // PNG magic number check: 89 50 4E 47 0D 0A 1A 0A
        assertEquals((byte) 0x89, qrCode[0]);
        assertEquals((byte) 0x50, qrCode[1]); // 'P'
        assertEquals((byte) 0x4E, qrCode[2]); // 'N'
        assertEquals((byte) 0x47, qrCode[3]); // 'G'
    }

    @Test
    void generateQrCode_ShouldThrowException_WhenTextIsNull() {
        // Act & Assert
        assertThrows(Exception.class, () -> qrCodeService.generateQrCode(null, 250, 250));
    }
}
