package com.github.labai.utils.convert;

import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author Augustus
 * created on 2019.04.09
 *
 * utility for dates conversion
 */
public final class LaConvDt {
    static final ConvDate convDate = ConvDate.instance;

    private LaConvDt() { }

    public static @Nullable LocalDate convToLocalDate(@Nullable LocalDate value) { return value == null ? null : convDate.convToLocalDate(value); }
    public static @Nullable LocalDate convToLocalDate(@Nullable LocalDateTime value) { return value == null ? null : convDate.convToLocalDate(value); }
    public static @Nullable LocalDate convToLocalDate(@Nullable OffsetDateTime value) { return value == null ? null : convDate.convToLocalDate(value); }
    public static @Nullable LocalDate convToLocalDate(@Nullable ZonedDateTime value) { return value == null ? null : convDate.convToLocalDate(value); }
    public static @Nullable LocalDate convToLocalDate(@Nullable Instant value) { return value == null ? null : convDate.convToLocalDate(value); }
    public static @Nullable LocalDate convToLocalDate(@Nullable Date value) { return value == null ? null : convDate.convToLocalDate(value); }

    public static @Nullable LocalDateTime convToLocalDateTime(@Nullable LocalDate value) { return value == null ? null : convDate.convToLocalDateTime(value); }
    public static @Nullable LocalDateTime convToLocalDateTime(@Nullable LocalDateTime value) { return value == null ? null : convDate.convToLocalDateTime(value); }
    public static @Nullable LocalDateTime convToLocalDateTime(@Nullable OffsetDateTime value) { return value == null ? null : convDate.convToLocalDateTime(value); }
    public static @Nullable LocalDateTime convToLocalDateTime(@Nullable ZonedDateTime value) { return value == null ? null : convDate.convToLocalDateTime(value); }
    public static @Nullable LocalDateTime convToLocalDateTime(@Nullable Instant value) { return value == null ? null : convDate.convToLocalDateTime(value); }
    public static @Nullable LocalDateTime convToLocalDateTime(@Nullable Date value) { return value == null ? null : convDate.convToLocalDateTime(value); }

    public static @Nullable OffsetDateTime convToOffsetDateTime(@Nullable LocalDate value) { return value == null ? null : convDate.convToOffsetDateTime(value); }
    public static @Nullable OffsetDateTime convToOffsetDateTime(@Nullable LocalDateTime value) { return value == null ? null : convDate.convToOffsetDateTime(value); }
    public static @Nullable OffsetDateTime convToOffsetDateTime(@Nullable OffsetDateTime value) { return value == null ? null : convDate.convToOffsetDateTime(value); }
    public static @Nullable OffsetDateTime convToOffsetDateTime(@Nullable ZonedDateTime value) { return value == null ? null : convDate.convToOffsetDateTime(value); }
    public static @Nullable OffsetDateTime convToOffsetDateTime(@Nullable Instant value) { return value == null ? null : convDate.convToOffsetDateTime(value); }
    public static @Nullable OffsetDateTime convToOffsetDateTime(@Nullable Date value) { return value == null ? null : convDate.convToOffsetDateTime(value); }

    public static @Nullable ZonedDateTime convToZonedDateTime(@Nullable LocalDate value) { return value == null ? null : convDate.convToZonedDateTime(value); }
    public static @Nullable ZonedDateTime convToZonedDateTime(@Nullable LocalDateTime value) { return value == null ? null : convDate.convToZonedDateTime(value); }
    public static @Nullable ZonedDateTime convToZonedDateTime(@Nullable OffsetDateTime value) { return value == null ? null : convDate.convToZonedDateTime(value); }
    public static @Nullable ZonedDateTime convToZonedDateTime(@Nullable ZonedDateTime value) { return value == null ? null : convDate.convToZonedDateTime(value); }
    public static @Nullable ZonedDateTime convToZonedDateTime(@Nullable Instant value) { return value == null ? null : convDate.convToZonedDateTime(value); }
    public static @Nullable ZonedDateTime convToZonedDateTime(@Nullable Date value) { return value == null ? null : convDate.convToZonedDateTime(value); }

    public static @Nullable Instant convToInstant(@Nullable LocalDate value) { return value == null ? null : convDate.convToInstant(value); }
    public static @Nullable Instant convToInstant(@Nullable LocalDateTime value) { return value == null ? null : convDate.convToInstant(value); }
    public static @Nullable Instant convToInstant(@Nullable OffsetDateTime value) { return value == null ? null : convDate.convToInstant(value); }
    public static @Nullable Instant convToInstant(@Nullable ZonedDateTime value) { return value == null ? null : convDate.convToInstant(value); }
    public static @Nullable Instant convToInstant(@Nullable Instant value) { return value == null ? null : convDate.convToInstant(value); }
    public static @Nullable Instant convToInstant(@Nullable Date value) { return value == null ? null : convDate.convToInstant(value); }

    public static @Nullable Date convToDate(@Nullable LocalDate value) { return value == null ? null : convDate.convToDate(value); }
    public static @Nullable Date convToDate(@Nullable LocalDateTime value) { return value == null ? null : convDate.convToDate(value); }
    public static @Nullable Date convToDate(@Nullable OffsetDateTime value) { return value == null ? null : convDate.convToDate(value); }
    public static @Nullable Date convToDate(@Nullable ZonedDateTime value) { return value == null ? null : convDate.convToDate(value); }
    public static @Nullable Date convToDate(@Nullable Instant value) { return value == null ? null : convDate.convToDate(value); }
    public static @Nullable Date convToDate(@Nullable Date value) { return value == null ? null : convDate.convToDate(value); }


    //
    // convToX functions.
    //   value should be not null (will not be checked)
    //
    // for internal LaUtils usage only!
    //
    static final class ConvDate {
        static final ConvDate instance = new ConvDate();
        private ConvDate() {
        }

        LocalDateTime dateToLocalDateTime(Date date) {
            // java.sql.Date is Date w/o time
            if (date instanceof java.sql.Date)
                return ((java.sql.Date) date).toLocalDate().atStartOfDay(); // w/o timezone?
            Instant instant = date.toInstant();
            return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        static Instant dateToInstant(Date date) {
            // for java.sql.Date .from() is not supported
            if (date instanceof java.sql.Date)
                return Instant.ofEpochMilli(((java.sql.Date) date).getTime());

            // for Timestamp nanos is used also
            if (date instanceof Timestamp)
                return ((Timestamp) date).toInstant();

            return date.toInstant();
        }

        LocalDateTime offsetToLocalDateTime(OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        OffsetDateTime localToOffsetDateTime(LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }

        public LocalDate convToLocalDate(Object value) {
            if (value instanceof LocalDate)
                return (LocalDate) value;
            if (value instanceof LocalDateTime)
                return ((LocalDateTime) value).toLocalDate();
            if (value instanceof OffsetDateTime)
                return offsetToLocalDateTime((OffsetDateTime) value).toLocalDate();
            if (value instanceof ZonedDateTime)
                return offsetToLocalDateTime(((ZonedDateTime) value).toOffsetDateTime()).toLocalDate();
            if (value instanceof Instant)
                return ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDate();
            if (value instanceof java.sql.Date)
                return Instant.ofEpochMilli(((java.sql.Date) value).getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (value instanceof Date)
                return dateToInstant((Date) value).atZone(ZoneId.systemDefault()).toLocalDate();
            return null;
        }

        public LocalDateTime convToLocalDateTime(Object value) {
            if (value instanceof LocalDate)
                return ((LocalDate) value).atStartOfDay();
            if (value instanceof LocalDateTime)
                return (LocalDateTime) value;
            if (value instanceof OffsetDateTime)
                return offsetToLocalDateTime((OffsetDateTime) value);
            if (value instanceof ZonedDateTime)
                return offsetToLocalDateTime(((ZonedDateTime) value).toOffsetDateTime());
            if (value instanceof Instant)
                return ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
            if (value instanceof Date)
                return dateToInstant((Date) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return null;
        }

        public OffsetDateTime convToOffsetDateTime(Object value) {
            if (value instanceof OffsetDateTime)
                return (OffsetDateTime) value;
            return convToZonedDateTime(value).toOffsetDateTime();
        }

        public ZonedDateTime convToZonedDateTime(Object value) {
            if (value instanceof LocalDate)
                return localToOffsetDateTime(((LocalDate) value).atStartOfDay()).toZonedDateTime();
            if (value instanceof LocalDateTime)
                return localToOffsetDateTime((LocalDateTime) value).toZonedDateTime();
            if (value instanceof OffsetDateTime)
                return ((OffsetDateTime) value).toZonedDateTime();
            if (value instanceof ZonedDateTime)
                return (ZonedDateTime) value;
            if (value instanceof Instant)
                return ((Instant) value).atZone(ZoneId.systemDefault());
            if (value instanceof Date)
                return dateToInstant((Date) value).atZone(ZoneId.systemDefault());
            return null;
        }

        public Instant convToInstant(Object value) {
            if (value instanceof Instant)
                return (Instant) value;
            if (value instanceof Date)
                return dateToInstant((Date) value);
            ZonedDateTime zdtm = convToZonedDateTime(value);
            if (zdtm == null)
                return null;
            return zdtm.toInstant();
        }

        public Date convToDate(Object value) {
            if (value instanceof Date)
                return new Date(((Date) value).getTime()); // new Date clean copy (e.g. w/o nanos?)
            Instant instant = convToInstant(value);
            if (instant == null)
                return null;
            return Date.from(instant);
        }
    }
}
