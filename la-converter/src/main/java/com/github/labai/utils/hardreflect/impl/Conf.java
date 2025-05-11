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

import com.github.labai.shaded.asm.Opcodes;

import java.util.concurrent.atomic.AtomicInteger;

/*
 * @author Augustus
 * created on 2023-02-14
 *
 * few functions to generate class for properties instead of reflection.
 * expect better performance.
 *
 * for internal usage!
 *
 */
final class Conf {
    static final String PACKAGE_NAME = "com.github.labai.hrgen";
    static final String PACKAGE_DIR_NAME = "com/github/labai/hrgen";
    static final String READER_IMPL_BASE = "PropReader";
    static final String READER_INTERFACE = "com/github/labai/utils/hardreflect/PropReader";
    static final String WRITER_IMPL_BASE = "PropWriter";
    static final String WRITER_INTERFACE = "com/github/labai/utils/hardreflect/PropWriter";
    static final String MULTIREADER_INTERFACE = "com/github/labai/utils/hardreflect/PropMultiReader";
    static final String MULTIWRITER_INTERFACE = "com/github/labai/utils/hardreflect/PropMultiWriter";
    static final String POJOCOPIER_INTERFACE = "com/github/labai/utils/hardreflect/LaHardCopy$PojoCopier";
    static final String POJOCOPIER_PARENT = "com/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier";
    static final String POJOCOPIER_IMPL_BASE = "PojoCopier";

    static final int JAVA_VERSION = Opcodes.V17;

    static final AtomicInteger counter = new AtomicInteger(0);

    static String genDotClassName(String classBaseName) {
        return PACKAGE_NAME + "." + classBaseName;
    }

    static String genSlashClassName(String classBaseName) {
        return PACKAGE_DIR_NAME + "/" + classBaseName;
    }

    static String genReaderDotClassName(String suffix) {
        return PACKAGE_NAME + "." + READER_IMPL_BASE + "$" + suffix;
    }

    static String genReaderSlashClassName(String suffix) {
        return PACKAGE_DIR_NAME + "/" + READER_IMPL_BASE + "$" + suffix;
    }

    static String genWriterDotClassName(String suffix) {
        return PACKAGE_NAME + "." + WRITER_IMPL_BASE + "$" + suffix;
    }

    static String genWriterSlashClassName(String suffix) {
        return PACKAGE_DIR_NAME + "/" + WRITER_IMPL_BASE + "$" + suffix;
    }

    // "java/lang/String"
    static String classSlashName(Class<?> clazz) {
        String name;
        if (clazz.isPrimitive()) {
            name = Utils.getRetTypeDes(clazz).boxedClass;
        } else {
            name = clazz.getName();
            if (name.endsWith(".class"))
                name = name.substring(0, name.length() - 6); // 6 - ".class"
        }
        return name.replace(".", "/");
    }

    // "Ljava/lang/String;"
    static String classAsType(Class<?> clazz) {
        return "L" + classSlashName(clazz) + ";";
    }

    static String makeSuffix(String className, String fieldName) {
        if (className.length() < 4)
            className += "xxxx";
        className = upperFirst(className.substring(0, 4).toLowerCase());
        if (fieldName.length() < 4)
            fieldName += "xxxx";
        fieldName = upperFirst(fieldName.substring(0, 4).toLowerCase());
        return className + fieldName + counter.incrementAndGet();
    }

    static String makeSuffixMulti(String className, int fieldCount) {
        if (className.length() < 4)
            className += "xxxx";
        className = upperFirst(className.substring(0, 4).toLowerCase());
        return className + "M" + fieldCount + "x" + counter.incrementAndGet();
    }

    private static String upperFirst(String str) {
        if (str == null || str.length() == 0) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
