package com.section.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private DateUtil() {}

    // LocalDate를 년.월.일 시간의 문자열로 변환
    public static String localDateTimeToStr(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
    }
}
