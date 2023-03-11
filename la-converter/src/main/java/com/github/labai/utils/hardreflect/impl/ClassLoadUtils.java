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
package com.github.labai.utils.hardreflect.impl;

import com.github.labai.utils.convert.ITypeConverter;
import com.github.labai.utils.hardreflect.LaHardCopy.PojoCopier;
import com.github.labai.utils.hardreflect.PropMultiReader;
import com.github.labai.utils.hardreflect.PropMultiWriter;
import com.github.labai.utils.hardreflect.PropReader;
import com.github.labai.utils.hardreflect.PropWriter;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

/*
 *
*/
final class ClassLoadUtils {
    private static DynamicClassLoader dynamicClassLoader = null;

    private static DynamicClassLoader getDynamicClassLoader() {
        if (dynamicClassLoader != null)
            return dynamicClassLoader;
        synchronized (ClassLoadUtils.class) {
            if (dynamicClassLoader != null)
                return dynamicClassLoader;
            dynamicClassLoader = new DynamicClassLoader(Thread.currentThread().getContextClassLoader());
        }
        return dynamicClassLoader;
    }

    static class DynamicClassLoader extends ClassLoader {
        public DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    static <T> PropReader<T> getPropReader(byte[] clazzBody, String className) {
        Class<?> clazz = getDynamicClassLoader().defineClass(className, clazzBody);
        try {
            return (PropReader<T>) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Could not initiate newly created class", e);
        }
    }

    static <T> PropWriter<T> getPropWriter(byte[] clazzBody, String className) {
        Class<?> clazz = getDynamicClassLoader().defineClass(className, clazzBody);
        try {
            return (PropWriter<T>) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Could not initiate newly created class", e);
        }
    }

    static PropMultiReader getPropMultiReader(byte[] clazzBody, String className) {
        Class<?> clazz = getDynamicClassLoader().defineClass(className, clazzBody);
        try {
            return (PropMultiReader) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Could not initiate newly created class", e);
        }
    }

    static PropMultiWriter getPropMultiWriter(byte[] clazzBody, String className) {
        Class<?> clazz = getDynamicClassLoader().defineClass(className, clazzBody);
        try {
            return (PropMultiWriter) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Could not initiate newly created class", e);
        }
    }

    @SuppressWarnings("unchecked")
    static <Fr, To> PojoCopier<Fr, To> getPojoCopier(byte[] clazzBody, String className, ITypeConverter<?, ?>[] convMapArr,  Function<Fr, ?>[] supplierMapArr) {
        Class<?> clazz = getDynamicClassLoader().defineClass(className, clazzBody);
        try {
            PojoCopier<Fr, To> pojo = (PojoCopier<Fr, To>) clazz.getDeclaredConstructor().newInstance();
            pojo.setTypeConverters(convMapArr);
            pojo.setDataSuppliers(supplierMapArr);
            return pojo;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Could not initiate newly created class", e);
        }
    }
}
