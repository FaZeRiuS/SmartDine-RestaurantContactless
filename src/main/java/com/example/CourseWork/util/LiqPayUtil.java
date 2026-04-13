package com.example.CourseWork.util;

import com.example.CourseWork.exception.BadRequestException;
import com.example.CourseWork.exception.ErrorMessages;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LiqPayUtil {

    private static final Pattern LIQPAY_ORDER_ID_PATTERN = Pattern.compile("^order_(\\d+)_\\d+$");

    private LiqPayUtil() {}

    /**
     * Extracts the database order ID from a LiqPay order_id string.
     * Expected format: order_{db_id}_{timestamp_or_uuid}
     *
     * @param liqpayOrderId the order_id from LiqPay
     * @return the integer database ID
     * @throws BadRequestException if the format is invalid
     */
    public static Integer extractDbOrderId(String liqpayOrderId) {
        if (liqpayOrderId == null) {
            throw new BadRequestException(ErrorMessages.ORDER_ID_REQUIRED);
        }
        Matcher m = LIQPAY_ORDER_ID_PATTERN.matcher(liqpayOrderId);
        if (!m.matches()) {
            throw new BadRequestException(ErrorMessages.INVALID_LIQPAY_ORDER_ID_FORMAT_PREFIX + liqpayOrderId);
        }
        return Integer.parseInt(m.group(1));
    }
}
