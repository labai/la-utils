package com.github.labai.utils.reflect;

import com.github.labai.utils.convert.ITypeConverter;
import com.github.labai.utils.convert.LaConvUtils;
import com.github.labai.utils.convert.LaConvUtils.ClassPairMap;
import com.github.labai.utils.convert.LaConvertException;
import com.github.labai.utils.convert.LaConverterRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Augustus
 *         created on 2016-06-14, 2018-12-26
 *
 * Methods:
 * 		static void assignValue (Object pojo, Field field, Object value)
 * 			assign [value] to [pojo].[field],
 * 			with allowed some common data-type conversions
 *
 * 	LaReflectException
 */
public class LaReflect {

    private final static LaConverterRegistry laConverter = new LaConverterRegistry();
    private final static ClassPairMap<ITypeConverter<?, ?>> converterCache = new ClassPairMap<>();

    public static class LaReflectException extends RuntimeException {
        public LaReflectException(String message) { super(message); }
        public LaReflectException(String message, Throwable cause) { super(message, cause); }
    }

    private LaReflect() { }

    /**
     * assign [value] to [pojo].[field]
     *
     * allowed some common data-type conversions - if type of value slightly differs from
     * field type, primitive data type conversion can be applied
     * (e.g. any type to String, numbers (e.g. Long vs Integer), dates, enums)
     *
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void assignValue (Object pojo, Field field, Object value) {
        try {
            Class<?> fldType = field.getType();
            final Object res;
            if (value == null) {
                res = LaConvUtils.convertNull(fldType);
            } else {
                Class<?> valType = value.getClass();
                ITypeConverter converter = converterCache.getOrPut(valType, fldType, () -> laConverter.getConverter(valType, fldType));
                if (converter == null)
                    throw new LaReflectException("Can't find converter for field '"+ field.getName() +"' (" + valType + " to " + fldType + ")");
                res = converter.convert(value);
            }
            field.set(pojo, res);
        } catch (IllegalArgumentException | IllegalAccessException | LaConvertException e ) {
            throw new LaReflectException("Error while assigning value to field '"+ field.getName() +"'", e);
        }
    }

    /**
     * copies fields directly (w/o setters), no deep copy.
     * field from super class will not be accessed (TODO?).
     */
    static void copyFields(Object source, Object target) {
        Map<String, Field> targetFieldMap = analyzeObjFields(target);
        copyFields(source, target, targetFieldMap);
    }

    //
    // private
    //

    static void copyFields(Object source, Object target, Map<String, Field> targetFieldMap) {
        try {
            for (Field sourceField : source.getClass().getDeclaredFields()) {
                sourceField.setAccessible(true);
                if (Modifier.isStatic(sourceField.getModifiers())) continue;
                Field tf = targetFieldMap.get(sourceField.getName());
                if (tf == null) continue;
                assignValue(target, tf, sourceField.get(source));
            }
        } catch (IllegalAccessException e) {
            throw new LaReflectException("Cannot copy fields", e);
        }
    }

    //
    // private
    //

    static Map<String, Field> analyzeObjFields(Object object) {
        if (object == null)
            throw new NullPointerException();
        return analyzeFields(object.getClass());
    }

    static Map<String, Field> analyzeFields(Class<?> clazz) {
        Map<String, Field> map = new LinkedHashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())) continue;
            map.put(field.getName(), field);
        }
        return map;
    }
}
