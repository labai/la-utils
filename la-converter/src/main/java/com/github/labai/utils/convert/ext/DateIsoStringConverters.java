package com.github.labai.utils.convert.ext;

import com.github.labai.utils.convert.ITypeConverter;
import com.github.labai.utils.convert.LaConvDt;
import com.github.labai.utils.convert.LaConverterRegistry;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.function.Function;

/*
 * @author Augustus
 * created on 2025-07-03
 */
public class DateIsoStringConverters {
    public static void registryDateStringConverters(LaConverterRegistry registry) {

        registerToString(registry, LocalDate.class, LocalDate::toString);
        registerToString(registry, LocalDateTime.class, LocalDateTime::toString);
        registerToString(registry, OffsetDateTime.class, OffsetDateTime::toString);
        registerToString(registry, ZonedDateTime.class, ZonedDateTime::toString);
        registerToString(registry, Timestamp.class, fr -> LaConvDt.convToOffsetDateTime(fr).toString());
        registerToString(registry, Date.class, fr -> LaConvDt.convToOffsetDateTime(fr).toString());
        registerToString(registry, java.sql.Date.class, fr -> LaConvDt.convToLocalDate(fr).toString()); // sqlDate about dates

        registerFromString(registry, LocalDate.class, LocalDate::parse);
        registerFromString(registry, LocalDateTime.class, LocalDateTime::parse);
        registerFromString(registry, OffsetDateTime.class, OffsetDateTime::parse);
        registerFromString(registry, ZonedDateTime.class, ZonedDateTime::parse);
        registerFromString(registry, Date.class, string -> LaConvDt.convToDate(stringToOffsetDateTime(string)));
        registerFromString(registry, Timestamp.class,
            string -> {
                Instant inst = stringToInstant(string);
                if (inst == null)
                    return null;
                return Timestamp.from(inst);
            });
        registerFromString(registry, java.sql.Date.class,
            string -> {
                Instant inst = stringToInstant(string);
                if (inst == null)
                    return null;
                return new java.sql.Date(inst.toEpochMilli());
            });
    }

    private static <Fr> void registerToString(LaConverterRegistry registry, Class<Fr> sourceType, Function<Fr, String> convFn) {
        registry.registerConverter(sourceType, String.class, withNullCheck(convFn));
    }

    private static <To> void registerFromString(LaConverterRegistry registry, Class<To> targetType, Function<String, To> convFn) {
        registry.registerConverter(String.class, targetType, withNullCheck(convFn));
    }

    private static <Fr, To> ITypeConverter<Fr, To> withNullCheck(Function<Fr, To> convFn) {
        return new ITypeConverter<Fr, To>() {
            @Override
            public @Nullable To convert(@Nullable Fr from) {
                return from == null ? null : convFn.apply(from);
            }
        };
    }

    private static OffsetDateTime stringToOffsetDateTime(String string) {
        Instant instant = stringToInstant(string);
        if (instant == null)
            return null;
        OffsetDateTime odt = LaConvDt.convToOffsetDateTime(instant);
        return odt;
    }

    private static Instant stringToInstant(String string) {
        if (string == null || string.isEmpty())
            return null;

        // quick check
        if (string.length() == 10) // yyyy-MM-dd
            return LaConvDt.convToInstant(LocalDate.parse(string));

        // try iso date-time
        try {
            LocalDateTime ldt = LocalDateTime.parse(string);
            return LaConvDt.convToInstant(ldt);
        } catch (DateTimeParseException e) {
            // continue
        }

        // try iso date-time with optional offset
        try {
            OffsetDateTime odt = OffsetDateTime.parse(string);
            return LaConvDt.convToInstant(odt);
        } catch (DateTimeParseException e) {
            // continue
        }

        // try iso date
        try {
            LocalDate ld = LocalDate.parse(string);
            return LaConvDt.convToInstant(ld);
        } catch (DateTimeParseException e) {
            // continue
        }

        // try ZonedDateTime
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(string);
            return LaConvDt.convToInstant(zdt);
        } catch (DateTimeParseException e) {
            // continue
        }

        return null;
    }
}
