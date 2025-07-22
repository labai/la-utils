package com.github.labai.utils.convert;

import com.github.labai.utils.convert.LaConvDt.ConvDate;

import java.math.BigDecimal;
import java.math.BigInteger;
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
 * created on 2022.11.18
 * <p>
 * common converters
 */
class StdConverters {
    private static final ConvDate convDate = LaConvDt.convDate;

    @SuppressWarnings({"rawtypes"})
    static <Fr, To> ITypeConverter chooseStdConverter(Class<Fr> sourceType, Class<To> targetType) {

        // numbers
        //
        if (BigDecimal.class.isAssignableFrom(targetType))
            return NumberConverters.converterToBigDecimal(sourceType);
        if (BigInteger.class.isAssignableFrom(targetType))
            return NumberConverters.converterToBigInteger(sourceType);
        if (targetType == Long.class || targetType == long.class)
            return NumberConverters.converterToLong(sourceType, targetType.isPrimitive());
        if (targetType == Integer.class || targetType == int.class)
            return NumberConverters.converterToInteger(sourceType, targetType.isPrimitive());
        if (targetType == Double.class || targetType == double.class)
            return NumberConverters.converterToDouble(sourceType, targetType.isPrimitive());
        if (targetType == Float.class || targetType == float.class)
            return NumberConverters.converterToFloat(sourceType, targetType.isPrimitive());
        if (targetType == Byte.class || targetType == byte.class)
            return NumberConverters.converterToByte(sourceType, targetType.isPrimitive());
        if (targetType == Short.class || targetType == short.class)
            return NumberConverters.converterToShort(sourceType, targetType.isPrimitive());

        // dates
        //
        if (targetType == LocalDate.class)
            return DateConverters.converterToLocalDate(sourceType);
        if (targetType == LocalDateTime.class)
            return DateConverters.converterToLocalDateTime(sourceType);
        if (targetType == OffsetDateTime.class)
            return DateConverters.converterToOffsetDateTime(sourceType);
        if (targetType == ZonedDateTime.class)
            return DateConverters.converterToZonedDateTime(sourceType);
        if (targetType == Instant.class)
            return DateConverters.converterToInstant(sourceType);
        if (targetType == java.sql.Date.class) {
            return value -> {
                Date date = convDate.convToDate(value);
                return (date == null) ? null : new java.sql.Date(date.getTime());
            };
        }
        if (targetType == Timestamp.class) {
            return value -> {
                Instant instant = convDate.convToInstant(value);
                return (instant == null) ? null : Timestamp.from(instant);
            };
        }
        if (targetType == Date.class) {
            return DateConverters.converterToDate(sourceType);
        }

        // misc
        //
        if (targetType == Character.class || targetType == char.class) {
            return MiscConverters.converterToChar(sourceType, targetType.isPrimitive());
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            return MiscConverters.converterToBoolean(sourceType, targetType.isPrimitive());
        }

        if (Enum.class.isAssignableFrom(targetType)) {
            return MiscConverters.converterToEnum(sourceType, targetType);
        }

        return null;
    }


    static class NumberConverters {
        // will return null
        static <F> ITypeConverter<F, BigDecimal> converterToBigDecimal(Class<F> fromClass) {
            if (fromClass == BigDecimal.class)
                return value -> (BigDecimal) value;
            if (fromClass == String.class)
                return value -> value == null ? null : new BigDecimal(value.toString());
            if (fromClass == double.class || fromClass == float.class || fromClass == Double.class || fromClass == Float.class)
                return value -> value == null ? null : BigDecimal.valueOf(((Number) value).doubleValue());
            if (fromClass == Long.class || fromClass == long.class)
                return value -> value == null ? null : new BigDecimal(((Number) value).longValue());
            if (fromClass == Integer.class || fromClass == int.class || fromClass == Short.class || fromClass == short.class || fromClass == Byte.class  || fromClass == byte.class)
                return value -> value == null ? null : new BigDecimal(((Number) value).intValue());
            if (fromClass == boolean.class || fromClass == Boolean.class)
                return value -> value == null ? null : (Boolean) value ? BigDecimal.ONE : BigDecimal.ZERO;
            if (fromClass == char.class || fromClass == Character.class)
                return null; // do not support?
            if (Number.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : new BigDecimal(value.toString()); // ?
            return null;
        }

        static <F> ITypeConverter<F, BigInteger> converterToBigInteger(Class<F> fromClass) {
            if (fromClass == BigInteger.class)
                return value -> (BigInteger) value;
            if (fromClass == String.class)
                return value -> value == null ? null : new BigInteger(value.toString());
            if (fromClass == boolean.class || fromClass == Boolean.class)
                return value -> value == null ? null : (Boolean) value ? BigInteger.ONE : BigInteger.ZERO;
            if (fromClass == char.class || fromClass == Character.class)
                return null; // do not support?
            if (fromClass.isPrimitive())
                return value -> BigInteger.valueOf(((Number) value).longValue());
            if (Number.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : BigInteger.valueOf(((Number) value).longValue());
            return null;
        }

        static <F> ITypeConverter<F, Integer> converterToInteger(Class<F> fromClass, boolean isTargetPrimitive) {
            if (isTargetPrimitive) {
                ITypeConverter<F, Integer> convFn = converterToInteger(fromClass, false);
                if (convFn == null)
                    return null;
                return value -> value == null ? 0 : convFn.convert(value);
            }
            if (fromClass == Integer.class || fromClass == int.class)
                return value -> (Integer) value;
            if (fromClass == String.class)
                return value -> value == null ? null : Integer.parseInt(value.toString());
            if (fromClass == boolean.class || fromClass == Boolean.class)
                return value -> value == null ? null : (Boolean) value ? 1 : 0;
            if (fromClass == char.class || fromClass == Character.class)
                return null; // do not support? or '(int)(Character) value' ?
            if (fromClass.isPrimitive())
                return value -> ((Number) value).intValue();
            if (Number.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : ((Number) value).intValue();
            return null;
        }

        static <F> ITypeConverter<F, Long> converterToLong(Class<F> fromClass, boolean isTargetPrimitive) {
            if (isTargetPrimitive) {
                ITypeConverter<F, Long> convFn = converterToLong(fromClass, false);
                if (convFn == null)
                    return null;
                return value -> value == null ? 0L : convFn.convert(value);
            }
            if (fromClass == Long.class || fromClass == long.class)
                return value -> (Long) value;
            if (fromClass == String.class)
                return value -> value == null ? null : Long.parseLong(value.toString());
            if (fromClass == boolean.class || fromClass == Boolean.class)
                return value -> value == null ? null : (Boolean) value ? 1L : 0L;
            if (fromClass == char.class || fromClass == Character.class)
                return null; // do not support?
            if (fromClass.isPrimitive())
                return value -> ((Number) value).longValue();
            if (Number.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : ((Number) value).longValue();
            return null;
        }

        static <F> ITypeConverter<F, Double> converterToDouble(Class<F> fromClass, boolean isTargetPrimitive) {
            if (isTargetPrimitive) {
                ITypeConverter<F, Double> convFn = converterToDouble(fromClass, false);
                if (convFn == null)
                    return null;
                return value -> value == null ? 0.0 : convFn.convert(value);
            }
            if (fromClass == double.class || fromClass == Double.class)
                return value -> (Double) value;
            if (fromClass == String.class)
                return value -> value == null ? null : Double.valueOf(value.toString());
            if (fromClass == boolean.class || fromClass == Boolean.class)
                return value -> value == null ? null : (Boolean) value ? 1.0 : 0.0;
            if (fromClass == char.class || fromClass == Character.class)
                return null; // do not support?
            if (fromClass.isPrimitive())
                return value -> ((Number) value).doubleValue();
            if (Number.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : ((Number) value).doubleValue();
            return null;
        }

        static <F> ITypeConverter<F, Float> converterToFloat(Class<F> fromClass, boolean isTargetPrimitive) {
            if (isTargetPrimitive) {
                ITypeConverter<F, Float> convFn = converterToFloat(fromClass, false);
                if (convFn == null)
                    return null;
                return value -> value == null ? 0.0f : convFn.convert(value);
            }
            if (fromClass == Float.class || fromClass == float.class)
                return value -> (Float) value;
            if (fromClass == String.class)
                return value -> value == null ? null : Float.valueOf(value.toString());
            if (fromClass == boolean.class || fromClass == Boolean.class)
                return value -> value == null ? null : (Boolean) value ? 1.0f : 0.0f;
            if (fromClass == char.class || fromClass == Character.class)
                return null; // do not support?
            if (fromClass.isPrimitive())
                return value -> ((Number) value).floatValue();
            if (Number.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : ((Number) value).floatValue();
            return null;
        }


        static <F> ITypeConverter<F, Short> converterToShort(Class<F> fromClass, boolean isTargetPrimitive) {
            if (isTargetPrimitive) {
                ITypeConverter<F, Short> fn = converterToShort(fromClass, false);
                if (fn == null)
                    return null;
                return value -> value == null ? 0 : fn.convert(value);
            }

            if (fromClass == Short.class || fromClass == short.class)
                return value -> (Short) value;
            if (fromClass == String.class)
                return value -> value == null ? null : Short.valueOf(value.toString());
            if (fromClass == boolean.class || fromClass == Boolean.class)
                return value -> value == null ? null : (Boolean) value ? (short) 1 : 0;
            if (fromClass == char.class || fromClass == Character.class)
                return null; // do not support?
            if (fromClass.isPrimitive())
                return value -> ((Number) value).shortValue();
            if (Number.class.isAssignableFrom(fromClass)) {
                return value -> value == null ? null : ((Number) value).shortValue();
            }
            return null;
        }

        static <F> ITypeConverter<F, Byte> converterToByte(Class<F> fromClass, boolean isTargetPrimitive) {
            if (isTargetPrimitive) {
                ITypeConverter<F, Byte> convFn = converterToByte(fromClass, false);
                if (convFn == null)
                    return null;
                return value -> value == null ? 0 : convFn.convert(value);
            }
            if (fromClass == Byte.class || fromClass == byte.class)
                return value -> (Byte) value;
            if (fromClass == String.class)
                return value -> value == null ? null : Byte.valueOf(value.toString());
            if (fromClass == boolean.class || fromClass == Boolean.class)
                return value -> value == null ? null : (Boolean) value ? (byte) 1 : 0;
            if (fromClass == char.class || fromClass == Character.class)
                return null; // do not support? or '(byte)(Character) value'?
            if (fromClass.isPrimitive())
                return value -> ((Number) value).byteValue();
            if (Number.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : ((Number) value).byteValue();
            return null;
        }

    }

    static class MiscConverters {

        static <F> ITypeConverter<F, Character> converterToChar(Class<F> fromClass, boolean isTargetPrimitive) {
            if (isTargetPrimitive) {
                ITypeConverter<F, Character> convFn = converterToChar(fromClass, false);
                if (convFn == null)
                    return null;
                return value -> value == null ? '\u0000' : convFn.convert(value);
            }
            if (fromClass == char.class || fromClass == Character.class)
                return value -> (Character) value;
            if (fromClass == String.class)
                return value -> value == null || ((String)value).isEmpty() ? null : ((String)value).charAt(0); // take first char of string?
            return null;
        }

        @SuppressWarnings("SimplifiableConditionalExpression")
        static <F> ITypeConverter<F, Boolean> converterToBoolean(Class<F> fromClass, boolean isTargetPrimitive) {
            if (isTargetPrimitive) {
                ITypeConverter<F, Boolean> convFn = converterToBoolean(fromClass, false);
                if (convFn == null)
                    return null;
                return value -> value == null ? false : convFn.convert(value);
            }
            if (fromClass == Boolean.class || fromClass == boolean.class)
                return value -> (Boolean) value;
            if (fromClass == char.class || fromClass == Character.class)
                return null; // do not support? or 'Y/N'?
            if (fromClass.isPrimitive())
                return value -> ((Number) value).longValue() != 0;
            if (Number.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : ((Number) value).longValue() != 0;
            if (fromClass == String.class)
                return value -> value == null ? null : Boolean.parseBoolean((String) value);
            return null;
        }

        static <F> ITypeConverter<F, Enum<?>> converterToEnum(Class<?> fromClass, Class<?> targetType) {
            if (fromClass == targetType)
                return value -> (Enum) value;
            if (fromClass == String.class) {
                return value -> {
                    try {
                        return value == null ? null : Enum.valueOf((Class<Enum>) targetType, value.toString());
                    } catch (IllegalArgumentException e) {
                        throw new LaConvertException("Error while assigning value to field.class '" + targetType.getCanonicalName() + "': " + e.getMessage());
                    }
                };
            }
            if (Enum.class.isAssignableFrom(fromClass)) { // other type of enum
                return value -> {
                    try {
                        return value == null ? null : Enum.valueOf((Class<Enum>) targetType, ((Enum<?>) value).name());
                    } catch (IllegalArgumentException e) {
                        throw new LaConvertException("Error while assigning value to field.class '" + targetType.getCanonicalName() + "': " + e.getMessage());
                    }
                };
            }
            return null;
        }
    }

    static class DateConverters {

        static <T> ITypeConverter<T, LocalDate> converterToLocalDate(Class<T> fromClass) {
            if (fromClass == LocalDate.class)
                return value -> (LocalDate) value;
            if (fromClass == LocalDateTime.class)
                return value -> value == null ? null : ((LocalDateTime) value).toLocalDate();
            if (fromClass == OffsetDateTime.class)
                return value -> value == null ? null : ((OffsetDateTime) value).toLocalDate();
            if (fromClass == ZonedDateTime.class)
                return value -> value == null ? null : ((ZonedDateTime) value).toLocalDate();
            if (fromClass == Instant.class)
                return value -> value == null ? null : ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDate();
            if (fromClass == java.sql.Date.class)
                return value -> value == null ? null : Instant.ofEpochMilli(((java.sql.Date) value).getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (Date.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : convDate.dateToInstant((Date) value).atZone(ZoneId.systemDefault()).toLocalDate();
            return null;
        }

        static <T> ITypeConverter<T, LocalDateTime> converterToLocalDateTime(Class<T> fromClass) {
            if (fromClass == LocalDate.class)
                return value -> value == null ? null : ((LocalDate) value).atStartOfDay();
            if (fromClass == LocalDateTime.class)
                return value -> (LocalDateTime) value;
            if (fromClass == OffsetDateTime.class)
                return value -> value == null ? null : ((OffsetDateTime) value).toLocalDateTime();
            if (fromClass == ZonedDateTime.class)
                return value -> value == null ? null : ((ZonedDateTime) value).toLocalDateTime();
            if (fromClass == Instant.class)
                return value -> value == null ? null : ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
            if (Date.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : convDate.dateToInstant((Date) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return null;
        }

        static <T> ITypeConverter<T, OffsetDateTime> converterToOffsetDateTime(Class<T> fromClass) {
            if (fromClass == OffsetDateTime.class)
                return value -> (OffsetDateTime) value;
            return value -> value == null ? null : convDate.convToZonedDateTime(value).toOffsetDateTime();
        }

        static <T> ITypeConverter<T, ZonedDateTime> converterToZonedDateTime(Class<T> fromClass) {
            if (fromClass == LocalDate.class)
                return value -> value == null ? null : ((LocalDate) value).atStartOfDay(ZoneId.systemDefault());
            if (fromClass == LocalDateTime.class)
                return value -> value == null ? null : ((LocalDateTime) value).atZone(ZoneId.systemDefault());
            if (fromClass == OffsetDateTime.class)
                return value -> value == null ? null : ((OffsetDateTime) value).toZonedDateTime();
            if (fromClass == ZonedDateTime.class)
                return value -> value == null ? null : (ZonedDateTime) value;
            if (fromClass == Instant.class)
                return value -> value == null ? null : ((Instant) value).atZone(ZoneId.systemDefault());
            if (Date.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : convDate.dateToLocalDateTime((Date) value).atZone(ZoneId.systemDefault());
            return null;
        }

        static <T> ITypeConverter<T, Instant> converterToInstant(Class<T> fromClass) {
            if (fromClass == Instant.class)
                return value -> (Instant) value;
            if (Date.class.isAssignableFrom(fromClass))
                return value -> value == null ? null : convDate.dateToInstant((Date) value);
            return value -> {
                if (value == null)
                    return null;
                ZonedDateTime zdtm = convDate.convToZonedDateTime(value);
                if (zdtm == null)
                    return null;
                return zdtm.toInstant();
            };
        }

        static <T> ITypeConverter<T, Date> converterToDate(Class<T> fromClass) {
            if (fromClass == Date.class)
                return value -> value == null ? null : new Date(((Date) value).getTime()); // new Date clean copy (e.g. w/o nanos?)
            return value -> {
                if (value == null)
                    return null;
                Instant instant = convDate.convToInstant(value);
                if (instant == null)
                    return null;
                return Date.from(instant);
            };
        }
    }
}
