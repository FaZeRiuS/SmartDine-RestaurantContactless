package com.example.CourseWork.service.security;

public interface HmacSignatureService {
    /**
     * Generates an HMAC-SHA256 signature for the given table number.
     *
     * @param tableNumber the table number to sign
     * @return the base64url encoded signature string
     */
    String signTableNumber(int tableNumber);

    /**
     * Verifies if the provided signature matches the table number.
     *
     * @param tableNumber the table number to verify
     * @param signature the signature to verify against
     * @return true if the signature is valid, false otherwise
     */
    boolean verifyTableNumber(int tableNumber, String signature);
}
