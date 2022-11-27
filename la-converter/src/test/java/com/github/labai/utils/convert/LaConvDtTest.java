package com.github.labai.utils.convert;

import com.github.labai.utils.convert.LaConvDt.ConvDate;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Augustus
 * created on 2018.12.26
 *
 * test LaConvert.ConvDate
 *
 */
public class LaConvDtTest {

    private static void assertEqLocalDate(LocalDate expected, Object value) {
        LocalDate val = ConvDate.convToLocalDate(value);
        assertEquals(expected, val);
    }

    @Test
    public void test_convDate_localDate() throws ParseException {
        LocalDate expected = LocalDate.parse("2018-11-08");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date1 = sdf.parse("2018-11-08 11:12:13");

        assertEqLocalDate(expected, LocalDate.parse("2018-11-08"));
        assertEqLocalDate(expected, LocalDateTime.parse("2018-11-08T11:12:13"));
        assertEqLocalDate(expected, date1);
        assertEqLocalDate(expected, new Timestamp(date1.getTime()));
        assertEqLocalDate(expected, new java.sql.Date(date1.getTime()));
    }

    @Test
    public void test_conv_dates() throws ParseException {
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
                testConvDate(ofr, oto);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked", "SimplifiableAssertion"})
    private void testConvDate(Object from, Object expected) {
        ITypeConverter converter = LaConverterRegistry.global.getConverter(from.getClass(), expected.getClass());
        Object converted = converter.convert(from);
        String msg = "failed conv " + from.getClass() + " to " + converted.getClass() + " (expected " + expected.getClass() + ")";
        assertEquals(msg, expected, converted);
    }

}
