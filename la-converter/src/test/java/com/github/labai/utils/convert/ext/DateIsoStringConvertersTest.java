package com.github.labai.utils.convert.ext;

import com.github.labai.utils.convert.IConverterResolver;
import com.github.labai.utils.convert.ITypeConverter;
import com.github.labai.utils.convert.LaConvDt;
import com.github.labai.utils.convert.LaConverterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Date;

import static com.github.labai.utils.convert.TestUtils.withTimeZone0200;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * @author Augustus
 * created on 2025-07-04
 */
public class DateIsoStringConvertersTest {
    IConverterResolver registry = LaConverterRegistry.global;

    @Test
    void test_conv_to_string() {
        withTimeZone0200(() -> {
            String str = "2018-11-08";
            assertDateToString(registry, LocalDate.parse(str), str);

            // shortest of:
            // uuuu-MM-dd'T'HH:mm
            // uuuu-MM-dd'T'HH:mm:ss
            // uuuu-MM-dd'T'HH:mm:ss.SSS
            // uuuu-MM-dd'T'HH:mm:ss.SSSSSS
            // uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS
            str = "2018-11-08T00:00";
            assertDateToString(registry, LocalDateTime.parse(str), str);


            str = "2018-11-08T00:00+02:00";
            assertDateToString(registry, OffsetDateTime.parse(str), str);

            str = "2018-11-08T00:00+02:00";
            Date dt = LaConvDt.convToDate(OffsetDateTime.parse(str));
            assertDateToString(registry, dt, str);
        });
    }

    @Test
    void test_localDate() {
        String str = "2018-11-08";
        assertDateToString(registry, LocalDate.parse(str), str);
        assertStringToDate(registry, LocalDate.parse(str), str);
    }

    @ParameterizedTest
    @CsvSource({
        "LDT, 2018-11-08T00:00:00.000000000, 2018-11-08T00:00",
        "LDT, 2018-11-08T00:00:01.000000000, 2018-11-08T00:00:01",
        "LDT, 2018-11-08T00:00:00.001000000, 2018-11-08T00:00:00.001",
        "LDT, 2018-11-08T00:00:00.000001000, 2018-11-08T00:00:00.000001",
        "LDT, 2018-11-08T00:00:00.000000001, 2018-11-08T00:00:00.000000001",
        "ODT, 2018-11-08T00:00:00.000000000+10:00, 2018-11-07T16:00", // adjusted to local time zone
    })
    void test_localDateTime(String type, String input, String expected) {
        withTimeZone0200(() -> {
            Temporal date = null;
            if (type.equals("LDT")) {
                date = LocalDateTime.parse(input);
            } else if (type.equals("ODT")) {
                date = LaConvDt.convToLocalDateTime(OffsetDateTime.parse(input));
            }
            assertDateToString(registry, date, expected);
            assertStringToDate(registry, date, expected);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08T00:00:00.000000000+10:00, 2018-11-08T00:00+10:00",
        "2018-11-08T00:00:01.000000000+10:00, 2018-11-08T00:00:01+10:00",
        "2018-11-08T00:00:00.001000000+10:00, 2018-11-08T00:00:00.001+10:00",
        "2018-11-08T00:00:00.000001000+10:00, 2018-11-08T00:00:00.000001+10:00",
        "2018-11-08T00:00:00.000000001+10:00, 2018-11-08T00:00:00.000000001+10:00",
        "2018-11-08T00:00:00.000000000Z, 2018-11-08T00:00Z",
        "2018-11-08T00:00:01.000000000Z, 2018-11-08T00:00:01Z",
        "2018-11-08T00:00:00.001000000Z, 2018-11-08T00:00:00.001Z",
        "2018-11-08T00:00:00.000001000Z, 2018-11-08T00:00:00.000001Z",
        "2018-11-08T00:00:00.000000001Z, 2018-11-08T00:00:00.000000001Z",
        "2018-11-08T00:00:00.000000001+00:00, 2018-11-08T00:00:00.000000001Z", // +00:00 -> Z
        "2018-11-08T00:00:00.000000001-12:10, 2018-11-08T00:00:00.000000001-12:10",
        "2018-11-08T00:00:00.000000001+10, 2018-11-08T00:00:00.000000001+10:00", // accept short offset
        "2018-11-08T00:00-10, 2018-11-08T00:00-10:00", // short offset with short time
        "2018-11-08T00:00Z, 2018-11-08T00:00Z",
    })
    void test_offsetDateTime(String input, String expected) {
        withTimeZone0200(() -> {
            assertDateToString(registry, OffsetDateTime.parse(input), expected);
            assertStringToDate(registry, OffsetDateTime.parse(input), expected);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08T00:00:00.000000000, 2018-11-08T00:00+02:00",
        "2018-11-08T00:00:01.000000000, 2018-11-08T00:00:01+02:00",
        "2018-11-08T00:00:00.001000000, 2018-11-08T00:00:00.001+02:00",
        "2018-11-08T00:00:00.000001000, 2018-11-08T00:00+02:00", // only .001 precision
        "2018-11-08T00:00:00.000000001, 2018-11-08T00:00+02:00", // only .001 precision
    })
    void test_date_from_local(String input, String expected) {
        withTimeZone0200(() -> {
            assertDateToString(registry, LaConvDt.convToDate(LocalDateTime.parse(input)), expected);
            assertStringToDate(registry, LaConvDt.convToDate(LocalDateTime.parse(input)), expected);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08T00:00:00.000000000+10:00, 2018-11-07T16:00+02:00",
        "2018-11-08T00:00:01.000000000+10:00, 2018-11-07T16:00:01+02:00", // value is converted to local date, based on local zone
        "2018-11-08T00:00:00.001000000+10:00, 2018-11-07T16:00:00.001+02:00",
        "2018-11-08T00:00:00.000001000+10:00, 2018-11-07T16:00+02:00",
        "2018-11-08T00:00:00.000000001+10:00, 2018-11-07T16:00+02:00",
        "2018-11-08T00:00:00.000000000Z, 2018-11-08T02:00+02:00", // local zone +02 (Vilnius).
        "2018-11-08T00:00:01.000000000Z, 2018-11-08T02:00:01+02:00",
        "2018-11-08T00:00:00.001000000Z, 2018-11-08T02:00:00.001+02:00",
        "2018-11-08T00:00:00.000001000Z, 2018-11-08T02:00+02:00",
        "2018-11-08T00:00:00.000000001Z, 2018-11-08T02:00+02:00",
        "2018-11-08T00:00:00.000000001-12:10, 2018-11-08T14:10+02:00",
    })
    void test_date_from_offset(String input, String expected) {
        withTimeZone0200(() -> {
            assertDateToString(registry, LaConvDt.convToDate(OffsetDateTime.parse(input)), expected);
            assertStringToDate(registry, LaConvDt.convToDate(OffsetDateTime.parse(input)), expected);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08T00:00:00.000000000+10:00, 2018-11-07T16:00+02:00",
        "2018-11-08T00:00:01.000000000+10:00, 2018-11-07T16:00:01+02:00", // value is converted to local date, based on local zone
        "2018-11-08T00:00:00.001000000+10:00, 2018-11-07T16:00:00.001+02:00",
        "2018-11-08T00:00:00.000001000+10:00, 2018-11-07T16:00:00.000001+02:00",
        "2018-11-08T00:00:00.000000001+10:00, 2018-11-07T16:00:00.000000001+02:00",
    })
    void test_timestamp_offset(String input, String expected) {
        withTimeZone0200(() -> {
            // If to use Timestamp.valueOf(OffsetDateTime.parse(input).toLocalDateTime())
            // it returns the original date (2018-11-08T00:00), w/o converting to local zone
            // thus differs from LaConvDt.convToDate() logic for Date
            // so we will use own builder
            OffsetDateTime odt = OffsetDateTime.parse(input);
            Date dt = LaConvDt.convToDate(odt);
            Timestamp ts = new Timestamp(dt.getTime());
            ts.setNanos(odt.getNano());

            assertDateToString(registry, ts, expected);
            assertStringToDate(registry, ts, expected);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08T00:00:01+10:00, 2018-11-07",  // converted to local date at zone +02!
        "2018-11-08T00:00:00Z, 2018-11-08",
        "2018-11-08T00:00:00+03:00, 2018-11-07",
        "2018-11-08T00:00:00+01:00, 2018-11-08",
        "2018-11-08T12:00:00Z, 2018-11-08",
    })
    void test_sqlDate_from_offset(String input, String expected) {
        withTimeZone0200(() -> {
            OffsetDateTime odt = OffsetDateTime.parse(input);
            Date dt = LaConvDt.convToDate(odt);
            java.sql.Date sqlDate = new java.sql.Date(dt.getTime());

            // even doesn't have "equals" :/, so our assertation doesn't work here
            // assertDateString(registry, sqlDate, expected);
            // assertStringDate(registry, sqlDate, expected);
            ITypeConverter<java.sql.Date, String> conv = registry.getConverter(java.sql.Date.class, String.class);
            assertEquals(expected, conv.convert(sqlDate));
            ITypeConverter<String, java.sql.Date> conv2 = registry.getConverter(String.class, java.sql.Date.class);
            assertEquals(expected, conv2.convert(expected).toString());
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void assertDateToString(IConverterResolver registry, T date, String expectedString) {
        ITypeConverter<T, String> conv = (ITypeConverter<T, String>) registry.getConverter(date.getClass(), String.class);
        assertEquals(expectedString, conv.convert(date));
    }

    @SuppressWarnings("unchecked")
    private <T> void assertStringToDate(IConverterResolver registry, T expectedDate, String string) {
        ITypeConverter<String, T> conv = (ITypeConverter<String, T>) registry.getConverter(String.class, expectedDate.getClass());
        assertEquals(expectedDate, conv.convert(string));
    }
}
