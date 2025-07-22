package com.github.labai.utils.convert;

import com.github.labai.utils.convert.LaConvDt.ConvDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.github.labai.utils.convert.LaConvDt.convDate;
import static com.github.labai.utils.convert.TestUtils.withTimeZone0200;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 * created on 2018.12.26
 *
 * test LaConvert.ConvDate
 *
 */
public class LaConvDtTest {

    @ParameterizedTest
    @ValueSource(strings = {"function", "lambda"})
    public void test_convDate_localDate(String engine) throws ParseException {
        LocalDate expected = LocalDate.parse("2018-11-08");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date1 = sdf.parse("2018-11-08 11:12:13");

        assertEqLocalDate(engine, expected, LocalDate.parse("2018-11-08"));
        assertEqLocalDate(engine, expected, LocalDateTime.parse("2018-11-08T11:12:13"));
        assertEqLocalDate(engine, expected, date1);
        assertEqLocalDate(engine, expected, new Timestamp(date1.getTime()));
        assertEqLocalDate(engine, expected, new java.sql.Date(date1.getTime()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"function", "lambda"})
    public void test_conv_dates(String engine) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date dt = sdf.parse("2018-11-08T00:00:00");
        List<Object> dates = Arrays.asList(
                LocalDate.parse("2018-11-08"),
                OffsetDateTime.parse("2018-11-08T00:00:00+02:00"), // systemZone - +02
                LocalDateTime.parse("2018-11-08T00:00:00"),
                dt,
                new java.sql.Date(dt.getTime()),
                new Timestamp(dt.getTime())
        );
        withTimeZone0200(() -> {
            for (Object ofr : dates) {
                for (Object oto : dates) {
                    testConvDate(engine, ofr, oto);
                }
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08T00:00:00+10:00, 2018-11-07T16:00",
    })
    void test_offset_to_localDateTime(String input, String expected) {
        withTimeZone0200(() -> {
            var convDate = ConvDate.instance;
            var odt = OffsetDateTime.parse(input);
            var ldt = convDate.offsetToLocalDateTime(odt);
            var expectedLdt = LocalDateTime.parse(expected);
            assertEquals(expectedLdt, ldt);

            // ZonedDateTime
            var zdt = ZonedDateTime.parse(input);
            var ldt2 = convDate.offsetToLocalDateTime(zdt.toOffsetDateTime());
            assertEquals(expectedLdt, ldt2);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08T00:00, 2018-11-08T00:00+02:00",
    })
    void test_local_to_offsetDateTime(String input, String expected) {
        withTimeZone0200(() -> {
            var convDate = ConvDate.instance;
            var ldt = LocalDateTime.parse(input);
            var odt = convDate.localToOffsetDateTime(ldt);
            var expectedOdt = OffsetDateTime.parse(expected);
            assertEquals(expectedOdt, odt);
            // ZonedDateTime
            var expectedLdt = ZonedDateTime.parse(expected);
            assertEquals(expectedLdt, odt.toZonedDateTime());
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08 00:00:00, 2018-11-08T00:00+02:00",
    })
    void test_date_to_offsetDateTime(String input, String expected) {
        withTimeZone0200(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date;
            try {
                date = sdf.parse(input);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            var convDate = ConvDate.instance;
            var odt = convDate.convToOffsetDateTime(date);
            var expectedOdt = OffsetDateTime.parse(expected);
            assertEquals(expectedOdt, odt);
            // ZonedDateTime
            var expectedLdt = ZonedDateTime.parse(expected);
            assertEquals(expectedLdt, odt.toZonedDateTime());
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08T00:00Z, 2018-11-08 02:00:00",
    })
    void test_offsetDateTime_to_date(String input, String expected) {
        withTimeZone0200(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date expectedDate;
            try {
                expectedDate = sdf.parse(expected);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            var convDate = ConvDate.instance;
            var odt = OffsetDateTime.parse(input);
            var date = convDate.convToDate(odt);
            assertEquals(expectedDate, date);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08 00:00:00, 2018-11-08T00:00",
    })
    void test_date_to_localDateTime(String input, String expected) {
        withTimeZone0200(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date;
            try {
                date = sdf.parse(input);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            var convDate = ConvDate.instance;
            var odt = convDate.convToLocalDateTime(date);
            var expectedLdt = LocalDateTime.parse(expected);
            assertEquals(expectedLdt, odt);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "2018-11-08T00:00, 2018-11-08 00:00:00",
    })
    void test_localDateTime_to_date(String input, String expected) {
        withTimeZone0200(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date expectedDate;
            try {
                expectedDate = sdf.parse(expected);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            var convDate = ConvDate.instance;
            var ldt = LocalDateTime.parse(input);
            var date = convDate.convToDate(ldt);
            assertEquals(expectedDate, date);
        });
    }

    //
    // private
    //

    private static void assertEqLocalDate(String engine, LocalDate expected, Object value) {
        LocalDate val;
        if (engine.equals("lambda")) {
            ITypeConverter converter = LaConverterRegistry.global.getConverter(value.getClass(), expected.getClass());
            val = (LocalDate) converter.convert(value);
        } else if (engine.equals("function")) {
            val = convDate.convToLocalDate(value);
        } else {
            throw new IllegalStateException("Unknown data type " + (expected == null ? "null" : expected.getClass().getName()));
        }
        assertEquals(expected, val);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void testConvDate(String engine, Object from, Object expected) {
        Object converted;
        if (engine.equals("lambda")) {
            ITypeConverter converter = LaConverterRegistry.global.getConverter(from.getClass(), expected.getClass());
            converted = converter.convert(from);
        } else if (engine.equals("function")) {
            converted = laConvDtConvTo(from, expected);
        } else {
            throw new IllegalStateException("Unknown data type " + (expected == null ? "null" : expected.getClass().getName()));
        }
        String msg = "failed conv " + from.getClass() + " to " + converted.getClass() + " (expected " + expected.getClass() + ")";
        assertEquals(expected, converted, msg);
    }

    private Object laConvDtConvTo(Object from, Object expected) {
        if (expected instanceof LocalDate)
            return convDate.convToLocalDate(from);
        if (expected instanceof LocalDateTime)
            return convDate.convToLocalDateTime(from);
        if (expected instanceof OffsetDateTime)
            return convDate.convToOffsetDateTime(from);
        if (expected instanceof ZonedDateTime)
            return convDate.convToZonedDateTime(from);
        if (expected instanceof Instant)
            return convDate.convToInstant(from);
        if (expected instanceof Timestamp)
            return new Timestamp(convDate.convToDate(from).getTime()); // hack, as LaConvDt doesn't support Timestamp
        if (expected instanceof Date)
            return convDate.convToDate(from);
        throw new IllegalStateException("Unknown data type " + (expected == null ? "null" : expected.getClass().getName()));
    }
}
