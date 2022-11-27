package com.github.labai.utils.convert;

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
	private LaConvDt() { }

	public static LocalDate convToLocalDate(LocalDate value) { return value == null ? null : ConvDate.convToLocalDate(value); }
	public static LocalDate convToLocalDate(LocalDateTime value) { return value == null ? null : ConvDate.convToLocalDate(value); }
	public static LocalDate convToLocalDate(OffsetDateTime value) { return value == null ? null : ConvDate.convToLocalDate(value); }
	public static LocalDate convToLocalDate(ZonedDateTime value) { return value == null ? null : ConvDate.convToLocalDate(value); }
	public static LocalDate convToLocalDate(Instant value) { return value == null ? null : ConvDate.convToLocalDate(value); }
	public static LocalDate convToLocalDate(Date value) { return value == null ? null : ConvDate.convToLocalDate(value); }

	public static LocalDateTime convToLocalDateTime(LocalDate value) { return value == null ? null : ConvDate.convToLocalDateTime(value); }
	public static LocalDateTime convToLocalDateTime(LocalDateTime value) { return value == null ? null : ConvDate.convToLocalDateTime(value); }
	public static LocalDateTime convToLocalDateTime(OffsetDateTime value) { return value == null ? null : ConvDate.convToLocalDateTime(value); }
	public static LocalDateTime convToLocalDateTime(ZonedDateTime value) { return value == null ? null : ConvDate.convToLocalDateTime(value); }
	public static LocalDateTime convToLocalDateTime(Instant value) { return value == null ? null : ConvDate.convToLocalDateTime(value); }
	public static LocalDateTime convToLocalDateTime(Date value) { return value == null ? null : ConvDate.convToLocalDateTime(value); }

	public static OffsetDateTime convToOffsetDateTime(LocalDate value) { return value == null ? null : ConvDate.convToOffsetDateTime(value); }
	public static OffsetDateTime convToOffsetDateTime(LocalDateTime value) { return value == null ? null : ConvDate.convToOffsetDateTime(value); }
	public static OffsetDateTime convToOffsetDateTime(OffsetDateTime value) { return value == null ? null : ConvDate.convToOffsetDateTime(value); }
	public static OffsetDateTime convToOffsetDateTime(ZonedDateTime value) { return value == null ? null : ConvDate.convToOffsetDateTime(value); }
	public static OffsetDateTime convToOffsetDateTime(Instant value) { return value == null ? null : ConvDate.convToOffsetDateTime(value); }
	public static OffsetDateTime convToOffsetDateTime(Date value) { return value == null ? null : ConvDate.convToOffsetDateTime(value); }

	public static ZonedDateTime convToZonedDateTime(LocalDate value) { return value == null ? null : ConvDate.convToZonedDateTime(value); }
	public static ZonedDateTime convToZonedDateTime(LocalDateTime value) { return value == null ? null : ConvDate.convToZonedDateTime(value); }
	public static ZonedDateTime convToZonedDateTime(OffsetDateTime value) { return value == null ? null : ConvDate.convToZonedDateTime(value); }
	public static ZonedDateTime convToZonedDateTime(ZonedDateTime value) { return value == null ? null : ConvDate.convToZonedDateTime(value); }
	public static ZonedDateTime convToZonedDateTime(Instant value) { return value == null ? null : ConvDate.convToZonedDateTime(value); }
	public static ZonedDateTime convToZonedDateTime(Date value) { return value == null ? null : ConvDate.convToZonedDateTime(value); }

	public static Instant convToInstant(LocalDate value) { return value == null ? null : ConvDate.convToInstant(value); }
	public static Instant convToInstant(LocalDateTime value) { return value == null ? null : ConvDate.convToInstant(value); }
	public static Instant convToInstant(OffsetDateTime value) { return value == null ? null : ConvDate.convToInstant(value); }
	public static Instant convToInstant(ZonedDateTime value) { return value == null ? null : ConvDate.convToInstant(value); }
	public static Instant convToInstant(Instant value) { return value == null ? null : ConvDate.convToInstant(value); }
	public static Instant convToInstant(Date value) { return value == null ? null : ConvDate.convToInstant(value); }

	public static Date convToDate(LocalDate value) { return value == null ? null : ConvDate.convToDate(value); }
	public static Date convToDate(LocalDateTime value) { return value == null ? null : ConvDate.convToDate(value); }
	public static Date convToDate(OffsetDateTime value) { return value == null ? null : ConvDate.convToDate(value); }
	public static Date convToDate(ZonedDateTime value) { return value == null ? null : ConvDate.convToDate(value); }
	public static Date convToDate(Instant value) { return value == null ? null : ConvDate.convToDate(value); }
	public static Date convToDate(Date value) { return value == null ? null : ConvDate.convToDate(value); }


	//
	// convToX functions.
	//   value should be not null (will not be checked)
	//
	// for internal LaUtils usage only!
	//
	public static class ConvDate {

		static Instant dateToInstant(Date date) {
			// for java.sql.Date .from() is not supported
			if (date instanceof java.sql.Date)
				return Instant.ofEpochMilli(((java.sql.Date) date).getTime());

			// for Timestamp nanos is used also
			if (date instanceof Timestamp)
				return ((Timestamp) date).toInstant();

			return date.toInstant();
		}

		public static LocalDate convToLocalDate(Object value) {
			if (value instanceof LocalDate)
				return (LocalDate) value;
			if (value instanceof LocalDateTime)
				return ((LocalDateTime) value).toLocalDate();
			if (value instanceof OffsetDateTime)
				return ((OffsetDateTime) value).toLocalDate();
			if (value instanceof ZonedDateTime)
				return ((ZonedDateTime) value).toLocalDate();
			if (value instanceof Instant)
				return ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDate();
			if (value instanceof java.sql.Date)
				return Instant.ofEpochMilli(((java.sql.Date) value).getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
			if (value instanceof Date)
				return dateToInstant((Date) value).atZone(ZoneId.systemDefault()).toLocalDate();
			return null;
		}

		public static LocalDateTime convToLocalDateTime(Object value) {
			if (value instanceof LocalDate)
				return ((LocalDate) value).atStartOfDay();
			if (value instanceof LocalDateTime)
				return (LocalDateTime) value;
			if (value instanceof OffsetDateTime)
				return ((OffsetDateTime) value).toLocalDateTime();
			if (value instanceof ZonedDateTime)
				return ((ZonedDateTime) value).toLocalDateTime();
			if (value instanceof Instant)
				return ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
			if (value instanceof Date)
				return dateToInstant((Date) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
			return null;
		}

		public static OffsetDateTime convToOffsetDateTime(Object value) {
			if (value instanceof OffsetDateTime)
				return (OffsetDateTime) value;
			return convToZonedDateTime(value).toOffsetDateTime();
		}

		public static ZonedDateTime convToZonedDateTime(Object value) {
			if (value instanceof LocalDate)
				return ((LocalDate) value).atStartOfDay(ZoneId.systemDefault());
			if (value instanceof LocalDateTime)
				return ((LocalDateTime) value).atZone(ZoneId.systemDefault());
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

		public static Instant convToInstant(Object value) {
			if (value instanceof Instant)
				return (Instant) value;
			if (value instanceof Date)
				return dateToInstant((Date) value);
			ZonedDateTime zdtm = convToZonedDateTime(value);
			if (zdtm == null)
				return null;
			return zdtm.toInstant();
		}

		public static Date convToDate(Object value) {
			if (value instanceof Date)
				return new Date(((Date) value).getTime()); // new Date clean copy (e.g. w/o nanos?)
			Instant instant = convToInstant(value);
			if (instant == null)
				return null;
			return Date.from(instant);
		}
	}
}
