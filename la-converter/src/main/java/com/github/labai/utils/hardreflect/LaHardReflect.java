/*
The MIT License (MIT)

Copyright (c) 2023 Augustus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.github.labai.utils.hardreflect;

import com.github.labai.utils.hardreflect.impl.LaHardReflectImpl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/*
 * @author Augustus
 * created on 2023-02-14
 *
 * few functions to generate class for properties instead of reflection.
 * expect better performance.
 *
 * For LaMapper only, any method can be changed in future!
 *
 */
public final class LaHardReflect {

    public static <T> PropReader<T> createReaderClass(Class<T> pojoClass, String fieldName) {
        return LaHardReflectImpl.createReaderClass(pojoClass, fieldName);
    }

    public static <T> PropReader<T> createReaderClass(Class<T> pojoClass, Method getter) {
        return LaHardReflectImpl.createReaderClass(pojoClass, getter);
    }

    public static <T> PropWriter<T> createWriterClass(Class<T> pojoClass, String fieldName) {
        return LaHardReflectImpl.createWriterClass(pojoClass, fieldName);
    }

    public static <T> PropWriter<T> createWriterClass(Class<T> pojoClass, Method getter) {
        return LaHardReflectImpl.createWriterClass(pojoClass, getter);
    }

    public static PropMultiReader createMultiReaderClass(Class<?> pojoClass, List<String> fields) {
        List<NameOrAccessor> props = fields.stream().map(NameOrAccessor::name).collect(Collectors.toList());
        return LaHardReflectImpl.createMultiReaderClass(pojoClass, props);
    }

    // for LaMapper only
    public static PropMultiReader createMultiReaderClassForProps(Class<?> pojoClass, List<NameOrAccessor> props) {
        return LaHardReflectImpl.createMultiReaderClass(pojoClass, props);
    }

    public static PropMultiWriter createMultiWriterClass(Class<?> pojoClass, List<String> fields) {
        List<NameOrAccessor> props = fields.stream().map(NameOrAccessor::name).collect(Collectors.toList());
        return LaHardReflectImpl.createMultiWriterClass(pojoClass, props);
    }

    // for LaMapper only
    public static PropMultiWriter createMultiWriterClassForProps(Class<?> pojoClass, List<NameOrAccessor> props) {
        return LaHardReflectImpl.createMultiWriterClass(pojoClass, props);
    }

    // for LaMapper only
    public static class NameOrAccessor {
        public final String name;
        public final Method accessor;

        private NameOrAccessor(String name, Method accessor) {
            this.name = name;
            this.accessor = accessor;
        }
        public static NameOrAccessor name(String name) {
            return new NameOrAccessor(name, null);
        }
        public static NameOrAccessor accessor(Method method) {
            return new NameOrAccessor(null, method);
        }
    }
}
