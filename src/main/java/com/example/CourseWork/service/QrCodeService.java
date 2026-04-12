package com.example.CourseWork.service;

public interface QrCodeService {
    /**
     * Generates a QR Code as a PNG byte array.
     *
     * @param text   The content to encode in the QR code (e.g., a URL)
     * @param width  The width of the generated QR code image
     * @param height The height of the generated QR code image
     * @return Byte array containing the PNG image
     */
    byte[] generateQrCode(String text, int width, int height);
}
