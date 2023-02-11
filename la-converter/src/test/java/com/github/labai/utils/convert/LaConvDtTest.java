package com.github.labai.utils.convert;

import com.github.labai.utils.convert.LaConvDt.ConvDate;
import org.junit.jupiter.params.ParameterizedTest;
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
                OffsetDateTime.parse("2018-11-08T00:00:00+02:00"), // systemZone - Vilnius
                LocalDateTime.parse("2018-11-08T00:00:00"),
                dt,
                new java.sql.Date(dt.getTime()),
                new Timestamp(dt.getTime())
        );

        for (Object ofr : dates) {
            for (Object oto : dates) {
                testConvDate(engine, ofr, oto);
            }
        }
    }

    private static void assertEqLocalDate(String engine, LocalDate expected, Object value) {
        LocalDate val;
        if (engine.equals("lambda")) {
            ITypeConverter converter = LaConverterRegistry.global.getConverter(value.getClass(), expected.getClass());
            val = (LocalDate) converter.convert(value);
        } else if (engine.equals("function")) {
            val = ConvDate.convToLocalDate(value);
        } else {
            throw new IllegalStateException("Unknown data type " + (expected == null ? "null" : expected.getClass().getName()));
        }
        assertEquals(expected, val);
    }

    @SuppressWarnings({"rawtypes", "unchecked", "SimplifiableAssertion"})
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
            return ConvDate.convToLocalDate(from);
        if (expected instanceof LocalDateTime)
            return ConvDate.convToLocalDateTime(from);
        if (expected instanceof OffsetDateTime)
            return ConvDate.convToOffsetDateTime(from);
        if (expected instanceof ZonedDateTime)
            return ConvDate.convToZonedDateTime(from);
        if (expected instanceof Instant)
            return ConvDate.convToInstant(from);
        if (expected instanceof Timestamp)
            return new Timestamp(ConvDate.convToDate(from).getTime()); // hack, as LaConvDt doesn't support Timestamp
        if (expected instanceof Date)
            return ConvDate.convToDate(from);
        throw new IllegalStateException("Unknown data type " + (expected == null ? "null" : expected.getClass().getName()));
    }

}
