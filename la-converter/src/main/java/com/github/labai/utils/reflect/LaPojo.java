package com.github.labai.utils.reflect;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Augustus
 *         created on 2019.04.13
 */
public class LaPojo {

    private static final Map<Class, Map<String, Field>> targetFieldMapCache = new HashMap<>();

    /**
     * copies fields directly (w/o setters), no deep copy.
     * field from super class will not be accessed (yet?).
     *
     * By default will use classes cache.
     */
    public static void copyFields(Object source, Object target) {
        copyFields(source, target, true);
    }

    //
    // if don't want to use cache (e.g. in dev mode) - set cacheClasses to false.
    //
    public static void copyFields(Object source, Object target, boolean cacheClasses) {
        if (cacheClasses == false) {
            LaReflect.copyFields(source, target);
            return;
        }

        LaReflect.copyFields(source, target, getCachedFieldsMap(target));
    }


    public static void copyFields(Object source, Object target, boolean cacheClasses, Collection<String> skipFields, Collection<String> copyFields) {
        if (cacheClasses == false) {
            LaReflect.copyFields(source, target);
            return;
        }

        LaReflect.copyFields(source, target, getCachedFieldsMap(target));
    }

    //
    // private
    //

    static Map<String, Field> getCachedFieldsMap(Object object) {
        if (object == null) throw new NullPointerException();
        Map<String, Field> fieldMap = targetFieldMapCache.get(object.getClass());
        if (fieldMap == null) {
            fieldMap = LaReflect.analyzeObjFields(object);
            synchronized (targetFieldMapCache) {
                targetFieldMapCache.put(object.getClass(), fieldMap);
            }
        }
        return fieldMap;
    }
}
