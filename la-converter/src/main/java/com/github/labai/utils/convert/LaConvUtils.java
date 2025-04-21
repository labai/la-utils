package com.github.labai.utils.convert;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author Augustus
 * created on 2022.11.19
 * <p>
 * for internal LaUtils usage only!
 */
public final class LaConvUtils {

    // primitives - fill 0 for numbers, false for boolean. Null for other
    public static @Nullable Object convertNull(@NotNull Class<?> targetType) {
        // primitives - fill 0 for numbers, false for boolean
        if (targetType.isPrimitive()) {
            if (targetType == byte.class) return 0;
            if (targetType == short.class) return 0;
            if (targetType == int.class) return 0;
            if (targetType == long.class) return 0L;
            if (targetType == float.class) return 0.0f;
            if (targetType == double.class) return 0.0d;
            if (targetType == char.class) return '\u0000';
            if (targetType == boolean.class) return false;
            throw new LaConvertException("Don't know this primitive class '" + targetType.getCanonicalName() + "'");
        } else {
            return null;
        }
    }

    public static class ClassPairMap<T> {
        private final Map<ClassPair, T> map = new ConcurrentHashMap<>();

        private static class ClassPair {
            final Class<?> source;
            final Class<?> target;

            ClassPair(Class<?> source, Class<?> target) {
                this.source = source;
                this.target = target;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ClassPair classPair = (ClassPair) o;
                return source.equals(classPair.source) && target.equals(classPair.target);
            }

            @Override
            public int hashCode() {
                return Objects.hash(source, target);
            }
        }

        public <Fr, To> T getOrPut(Class<Fr> sourceType, Class<To> targetType, Supplier<T> itemFn) {
            ClassPair key = new ClassPair(sourceType, targetType);
            T value = map.get(key);
            if (value != null)
                return value;
            synchronized (map) {
                return map.computeIfAbsent(key, k -> itemFn.get());
            }
        }

        public <Fr, To> T get(Class<Fr> sourceType, Class<To> targetType) {
            return map.get(new ClassPair(sourceType, targetType));
        }
    }
}
