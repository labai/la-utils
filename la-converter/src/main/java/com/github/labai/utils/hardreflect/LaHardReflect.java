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

import java.lang.reflect.Method;

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

    public static PropReader createReaderClass(Class<?> pojoClass, String fieldName) {
        return LaHardReflectImpl.createReaderClass(pojoClass, fieldName);
    }

    public static PropReader createReaderClass(Class<?> pojoClass, Method getter) {
        return LaHardReflectImpl.createReaderClass(pojoClass, getter);
    }

    public static PropWriter createWriterClass(Class<?> pojoClass, String fieldName) {
        return LaHardReflectImpl.createWriterClass(pojoClass, fieldName);
    }

    public static PropWriter createWriterClass(Class<?> pojoClass, Method getter) {
        return LaHardReflectImpl.createWriterClass(pojoClass, getter);
    }
}
