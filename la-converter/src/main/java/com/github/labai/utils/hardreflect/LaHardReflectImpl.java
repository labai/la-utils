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

import com.github.labai.shaded.asm.ClassWriter;
import com.github.labai.shaded.asm.MethodVisitor;
import com.github.labai.shaded.asm.Opcodes;
import com.github.labai.utils.hardreflect.LaHardReflect.NameOrAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    static final int JAVA_VERSION = Opcodes.V1_8;

    static final AtomicInteger counter = new AtomicInteger(0);

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
            name = LaHardReflectImpl.getRetTypeDes(clazz).boxedClass;
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

/* ********************************************************************
*/
final class LaHardReflectImpl {

    private static class ClassDef {
        final String name;
        final byte[] body;

        public ClassDef(String name, byte[] body) {
            this.name = name;
            this.body = body;
        }
    }

    static class RetType {
        final String descriptor;
        final int aload;
        final int areturn;
        final String boxedClass;
        final String boxerFn = "valueOf"; //
        final String unboxerFn; // e.g. intValue

        public RetType(String descriptor, int aload, int areturn, String boxedClass, String unboxerFn) {
            this.descriptor = descriptor;
            this.aload = aload;
            this.areturn = areturn;
            this.boxedClass = boxedClass == null ? null : "java/lang/" + boxedClass; // e.g. "java/lang/Integer"
            this.unboxerFn = unboxerFn;
        }

        String getBoxerDescriptor() {
            // e.g. "(I)Ljava/lang/Integer;"
            return "(" + descriptor + ")L" + boxedClass + ";";
        }

        String getUnboxerDescriptor() {
            // e.g. "()I"
            return "()" + descriptor;
        }

        String getUnboxedType(Class<?> returnType) {
            if (isPrimitive())
                return descriptor;
            return Conf.classAsType(returnType);
        }

        boolean isObject() {
            return boxedClass == null;
        }

        boolean isPrimitive() {
            return !isObject();
        }
    }

    static <T> PropReader<T> createReaderClass(Class<T> pojoClass, String fieldName) {
        Field field = getField(pojoClass, fieldName);

        String suffix = Conf.makeSuffix(pojoClass.getSimpleName(), fieldName);

        ClassDef def;
        if (field != null) {
            def = generateReaderClass(pojoClass, fieldName, field.getType(), suffix, false);
        } else {
            Method getter = getGetter(pojoClass, fieldName);
            if (getter == null)
                return null;
            def = generateReaderClass(pojoClass, getter.getName(), getter.getReturnType(), suffix, true);
        }

        return ClassLoadUtils.getPropReader(def.body, def.name);
    }

    static <T> PropReader<T> createReaderClass(Class<T> pojoClass, Method getter) {
        if (getter.getParameterCount() != 0)
            throw new IllegalArgumentException("Getter must not have parameters");
        if (Modifier.isStatic(getter.getModifiers()))
            throw new IllegalArgumentException("Getter must not be a static method");
        String name = getter.getName();
        if (name.startsWith("get"))
            name = name.substring(3);
        else if (name.startsWith("is"))
            name = name.substring(2);
        String suffix = Conf.makeSuffix(pojoClass.getSimpleName(), name);
        ClassDef def = generateReaderClass(pojoClass, getter.getName(), getter.getReturnType(), suffix, true);
        return ClassLoadUtils.getPropReader(def.body, def.name);
    }

    static <T> PropWriter<T> createWriterClass(Class<T> pojoClass, String fieldName) {
        Field field = getField(pojoClass, fieldName);

        String suffix = Conf.makeSuffix(pojoClass.getSimpleName(), fieldName);

        ClassDef def;
        if (field != null) {
            def = generateWriterClass(pojoClass, fieldName, field.getType(), suffix, false);
        } else {
            Method setter = getSetter(pojoClass, fieldName);
            if (setter == null)
                return null;
            def = generateWriterClass(pojoClass, setter.getName(), setter.getParameterTypes()[0], suffix, true);
        }
        return ClassLoadUtils.getPropWriter(def.body, def.name);
    }

    static <T> PropWriter<T> createWriterClass(Class<T> pojoClass, Method setter) {
        if (setter.getParameterCount() != 1)
            throw new IllegalArgumentException("Setter must have 1 parameter");
        if (Modifier.isStatic(setter.getModifiers()))
            throw new IllegalArgumentException("Setter must not be a static method");
        String name = setter.getName();
        if (name.startsWith("set"))
            name = name.substring(3);
        String suffix = Conf.makeSuffix(pojoClass.getSimpleName(), name);
        ClassDef def = generateWriterClass(pojoClass, setter.getName(), setter.getParameterTypes()[0], suffix, true);
        return ClassLoadUtils.getPropWriter(def.body, def.name);
    }

    // props - can be a name (String) or a getter (Method)
    static PropMultiReader createMultiReaderClass(Class<?> pojoClass, List<NameOrAccessor> props) {
        List<PropDef> propDefs = new ArrayList<>();
        for (NameOrAccessor prop : props) {
            Field field = null;
            Method getter = null;
            if (prop.name != null) {
                field = getField(pojoClass, prop.name);
                if (field == null) {
                    getter = getGetter(pojoClass, prop.name);
                    if (getter == null)
                        return null;
                }
            } else if (prop.accessor != null) {
                getter = prop.accessor;
            } else {
                return null;
            }
            PropDef propDef;
            if (field != null) {
                propDef = new PropDef(field.getName(), field.getType(), false);
            } else {
                propDef = new PropDef(getter.getName(), getter.getReturnType(), true);
            }

            propDefs.add(propDef);
        }
        String suffix = Conf.makeSuffixMulti(pojoClass.getSimpleName(), props.size());
        ClassDef def = generateMultiReaderClass(pojoClass, propDefs, suffix);

        return ClassLoadUtils.getPropMultiReader(def.body, def.name);
    }

    static PropMultiWriter createMultiWriterClass(Class<?> pojoClass, List<NameOrAccessor> props) {
        List<PropDef> propDefs = new ArrayList<>();
        for (NameOrAccessor prop : props) {
            Field field = null;
            Method setter = null;
            if (prop.name != null) {
                field = getField(pojoClass, prop.name);
                if (field == null) {
                    setter = getSetter(pojoClass, prop.name);
                    if (setter == null)
                        return null;
                }
            } else if (prop.accessor != null) {
                setter = prop.accessor;
            } else {
                return null;
            }

            PropDef propDef;
            if (field != null) {
                propDef = new PropDef(field.getName(), field.getType(), false);
            } else {
                propDef = new PropDef(setter.getName(), setter.getParameterTypes()[0], true);
            }
            propDefs.add(propDef);
        }
        String suffix = Conf.makeSuffixMulti(pojoClass.getSimpleName(), props.size());
        ClassDef def = generateMultiWriterClass(pojoClass, propDefs, suffix);

        return ClassLoadUtils.getPropMultiWriter(def.body, def.name);
    }
    private static <T> ClassDef generateReaderClass(Class<T> pojoClass, String fieldOrMethodName, Class<?> propType, String nameSuffix, boolean isGetter) {
        String slashClassName = Conf.genReaderSlashClassName(nameSuffix);
        String dotClassName = Conf.genReaderDotClassName(nameSuffix);

        RetType retType = getRetTypeDes(propType);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(Conf.JAVA_VERSION,         // Java version
            Opcodes.ACC_PUBLIC,             // public class
            slashClassName,                 // package and name
            null,                           // signature (null means not generic)
            "java/lang/Object",             // superclass
            new String[]{Conf.READER_INTERFACE}); // interfaces

        // Build constructor
        //
        MethodVisitor con = cw.visitMethod(
            Opcodes.ACC_PUBLIC,                 // public method
            "<init>",                           // method name
            "()V",                              // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        con.visitCode();                        // Start the code for this method
        con.visitVarInsn(Opcodes.ALOAD, 0);  // Load "this" onto the stack

        con.visitMethodInsn(Opcodes.INVOKESPECIAL,  // Invoke an instance method (non-virtual)
            "java/lang/Object",                 // Class on which the method is defined
            "<init>",                           // Name of the method
            "()V",                              // Descriptor
            false);                             // Is this class an interface?

        con.visitInsn(Opcodes.RETURN);          // End the constructor method
        con.visitMaxs(1, 1);            // Specify max stack and local vars

        // Build 'readVal' method
        //
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,                 // public method
            "readVal",                          // name
            "(L" + Conf.classSlashName(pojoClass) + ";)Ljava/lang/Object;", // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);    // Load value onto stack
        if (isGetter) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Conf.classSlashName(pojoClass), fieldOrMethodName, "()" + retType.getUnboxedType(propType), false);
        } else { // field
            mv.visitFieldInsn(Opcodes.GETFIELD, Conf.classSlashName(pojoClass), fieldOrMethodName, retType.getUnboxedType(propType));
        }
        if (retType.isPrimitive()) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, retType.boxedClass, retType.boxerFn, retType.getBoxerDescriptor(), false);
        }
        mv.visitInsn(Opcodes.ARETURN);                      // Return typed value from top of stack
        mv.visitMaxs(1, 2);                         // Specify max stack and local vars

        // build synthetic method (version for recall main method)
        //
        // MethodVisitor mv2 = cw.visitMethod(
        //     Opcodes.ACC_PUBLIC + Opcodes.ACC_SYNTHETIC,
        //     "readVal2",
        //     "(Ljava/lang/Object;)Ljava/lang/Object;",
        //     null,
        //     null);
        // mv2.visitCode();
        // mv2.visitVarInsn(Opcodes.ALOAD, 0);
        // mv2.visitVarInsn(Opcodes.ALOAD, 1);
        // mv2.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(pojoClass));
        // mv2.visitMethodInsn(Opcodes.INVOKEVIRTUAL, slashClassName, "readVal", "(L" + Conf.classSlashName(pojoClass) + ";)Ljava/lang/Object;", false);
        // mv2.visitInsn(Opcodes.ARETURN);
        // mv2.visitMaxs(2, 2);

        // build synthetic method with Object as parameter
        //
        mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC + Opcodes.ACC_SYNTHETIC,
            "readVal",                          // name
            "(Ljava/lang/Object;)Ljava/lang/Object;", // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1); // Load value onto stack
        mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(pojoClass));
        if (isGetter) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Conf.classSlashName(pojoClass), fieldOrMethodName, "()" + retType.getUnboxedType(propType), false);
        } else { // field
            mv.visitFieldInsn(Opcodes.GETFIELD, Conf.classSlashName(pojoClass), fieldOrMethodName, retType.getUnboxedType(propType));
        }
        if (retType.isPrimitive()) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, retType.boxedClass, retType.boxerFn, retType.getBoxerDescriptor(), false);
        }
        mv.visitInsn(Opcodes.ARETURN); // Return typed value from top of stack
        mv.visitMaxs(1, 2); // Specify max stack and local vars

        cw.visitEnd(); // Finish the class definition

        return new ClassDef(dotClassName, cw.toByteArray());
    }

    private static ClassDef generateWriterClass(Class<?> pojoClass, String fieldOrMethodName, Class<?> propType, String nameSuffix, boolean isSetter) {
        String slashClassName = Conf.genWriterSlashClassName(nameSuffix);
        String dotClassName = Conf.genWriterDotClassName(nameSuffix);

        RetType retType = getRetTypeDes(propType);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(Conf.JAVA_VERSION,        // Java version
            Opcodes.ACC_PUBLIC,             // public class
            slashClassName,                 // package and name
            null,                           // signature (null means not generic)
            "java/lang/Object",             // superclass
            new String[]{Conf.WRITER_INTERFACE}); // interfaces

        // Build constructor
        //
        MethodVisitor con = cw.visitMethod(
            Opcodes.ACC_PUBLIC,                 // public method
            "<init>",                           // method name
            "()V",                              // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        con.visitCode();                        // Start the code for this method
        con.visitVarInsn(Opcodes.ALOAD, 0);  // Load "this" onto the stack

        con.visitMethodInsn(Opcodes.INVOKESPECIAL,  // Invoke an instance method (non-virtual)
            "java/lang/Object",                 // Class on which the method is defined
            "<init>",                           // Name of the method
            "()V",                              // Descriptor
            false);                             // Is this class an interface?

        con.visitInsn(Opcodes.RETURN);                      // End the constructor method
        con.visitMaxs(1, 1);            // Specify max stack and local vars

        // Build 'writeVal' method
        //
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,                 // public method
            "writeVal",                          // name
            "(L" + Conf.classSlashName(pojoClass) + ";Ljava/lang/Object;)V", // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);                  // Load value onto stack
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        //    CHECKCAST java/lang/String
        mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(propType));
        if (retType.isPrimitive()) {
            // INVOKEVIRTUAL java/lang/Integer.intValue ()I
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, retType.boxedClass, retType.unboxerFn, retType.getUnboxerDescriptor(), false);
        }
        if (isSetter) {
            // INVOKEVIRTUAL package1/ExampleWriter3$IntVal.setIntValue (I)V
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Conf.classSlashName(pojoClass), fieldOrMethodName, "(" + retType.getUnboxedType(propType) + ")V", false);
        } else { // field
            // PUTFIELD package1/ExampleWriter1$IntVal.intValue : I
            mv.visitFieldInsn(Opcodes.PUTFIELD, Conf.classSlashName(pojoClass), fieldOrMethodName, retType.getUnboxedType(propType));
        }
        mv.visitInsn(Opcodes.RETURN);                      // Return typed value from top of stack
        mv.visitMaxs(2, 3);                         // Specify max stack and local vars

        // // Build synthetic 'writeVal' method (recalls)
        //
        // // public synthetic bridge writeVal(Ljava/lang/Object;Ljava/lang/Object;)V
        // MethodVisitor mv2 = cw.visitMethod(
        //     Opcodes.ACC_PUBLIC + Opcodes.ACC_SYNTHETIC + Opcodes.ACC_BRIDGE,
        //     "writeVal",                          // name
        //     "(Ljava/lang/Object;Ljava/lang/Object;)V", // descriptor
        //     null,                               // signature (null means not generic)
        //     null);                              // exceptions (array of strings)
        // mv2.visitCode();
        // mv2.visitVarInsn(Opcodes.ALOAD, 0);
        // mv2.visitVarInsn(Opcodes.ALOAD, 1);
        // //    CHECKCAST package1/ExampleWriter3$IntVal
        // mv2.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(pojoClass));
        // mv2.visitVarInsn(Opcodes.ALOAD, 2);
        // //    INVOKEVIRTUAL package1/ExampleWriter3.writeVal (Lpackage1/ExampleWriter3$IntVal;Ljava/lang/Object;)V
        // mv2.visitMethodInsn(Opcodes.INVOKEVIRTUAL, slashClassName, "writeVal", "(L" + Conf.classSlashName(pojoClass) + ";Ljava/lang/Object;)V", false);
        // mv2.visitInsn(Opcodes.RETURN);
        // mv2.visitMaxs(3, 3);

        // Build synthetic 'writeVal' method for Object parameter
        //
        mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC + Opcodes.ACC_SYNTHETIC + Opcodes.ACC_BRIDGE,
            "writeVal",                          // name
            "(Ljava/lang/Object;Ljava/lang/Object;)V", // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);                  // Load value onto stack
        // CHECKCAST package1/ExampleWriter1$StrVal
        mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(pojoClass));
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        // CHECKCAST java/lang/String
        mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(propType));
        if (retType.isPrimitive()) {
            // INVOKEVIRTUAL java/lang/Integer.intValue ()I
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, retType.boxedClass, retType.unboxerFn, retType.getUnboxerDescriptor(), false);
        }
        if (isSetter) {
            // INVOKEVIRTUAL package1/ExampleWriter3$IntVal.setIntValue (I)V
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Conf.classSlashName(pojoClass), fieldOrMethodName, "(" + retType.getUnboxedType(propType) + ")V", false);
        } else { // field
            // PUTFIELD package1/ExampleWriter1$IntVal.intValue : I
            mv.visitFieldInsn(Opcodes.PUTFIELD, Conf.classSlashName(pojoClass), fieldOrMethodName, retType.getUnboxedType(propType));
        }
        mv.visitInsn(Opcodes.RETURN);                      // Return typed value from top of stack
        mv.visitMaxs(2, 3);                         // Specify max stack and local vars

        cw.visitEnd(); // Finish the class definition

        return new ClassDef(dotClassName, cw.toByteArray());
    }

    private static class PropDef {
        String fieldOrMethodName;
        Class<?> propType;
        boolean isAccessor; // getter or setter
        public PropDef(String fieldOrMethodName, Class<?> propType, boolean isAccessor) {
            this.fieldOrMethodName = fieldOrMethodName;
            this.propType = propType;
            this.isAccessor = isAccessor;
        }
    }

    //    public Object[] readVals(Object pojo) {
    //        Object[] res = new Object[3];
    //        Source3 src = ((Source3)pojo);
    //        int counter = 0;
    //        res[counter++] = src.getA1();
    //        res[counter++] = src.getA2();
    //        res[counter++] = src.getA3();
    //        return res;
    //    }
    private static <T> ClassDef generateMultiReaderClass(Class<T> pojoClass, List<PropDef> propDefList, String nameSuffix) {
        String slashClassName = Conf.genReaderSlashClassName(nameSuffix);
        String dotClassName = Conf.genReaderDotClassName(nameSuffix);


        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(Conf.JAVA_VERSION,         // Java version
            Opcodes.ACC_PUBLIC,             // public class
            slashClassName,                 // package and name
            null,                           // signature (null means not generic)
            "java/lang/Object",             // superclass
            new String[]{Conf.MULTIREADER_INTERFACE}); // interfaces

        // Build constructor
        //
        MethodVisitor con = cw.visitMethod(
            Opcodes.ACC_PUBLIC,                 // public method
            "<init>",                           // method name
            "()V",                              // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        con.visitCode();                        // Start the code for this method
        con.visitVarInsn(Opcodes.ALOAD, 0);  // Load "this" onto the stack

        con.visitMethodInsn(Opcodes.INVOKESPECIAL,  // Invoke an instance method (non-virtual)
            "java/lang/Object",                 // Class on which the method is defined
            "<init>",                           // Name of the method
            "()V",                              // Descriptor
            false);                             // Is this class an interface?

        con.visitInsn(Opcodes.RETURN);          // End the constructor method
        con.visitMaxs(1, 1);            // Specify max stack and local vars

        // Build 'readVals' method
        //
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,                 // public method
            "readVals",                          // name
            "(Ljava/lang/Object;)[Ljava/lang/Object;", // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        mv.visitCode();

        //    BIPUSH 13
        //    ANEWARRAY java/lang/Object
        //    ASTORE 2

        putInt(mv, propDefList.size());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        //    ALOAD 1
        //    CHECKCAST package1/Source3
        //    ASTORE 3
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(pojoClass));
        mv.visitVarInsn(Opcodes.ASTORE, 3);

        //    ICONST_0
        //    ISTORE 4
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 4);

        for (PropDef propDef : propDefList) {
            //    ALOAD 2
            //    ILOAD 4
            //    IINC 4 1
            //    ALOAD 3
            //    INVOKEVIRTUAL package1/Source3.getA1 ()Ljava/lang/String;
            //    AASTORE
            RetType retType = getRetTypeDes(propDef.propType);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ILOAD, 4);
            mv.visitIincInsn(4, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            if (propDef.isAccessor) {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Conf.classSlashName(pojoClass), propDef.fieldOrMethodName, "()" + retType.getUnboxedType(propDef.propType), false);
            } else { // field
                mv.visitFieldInsn(Opcodes.GETFIELD, Conf.classSlashName(pojoClass), propDef.fieldOrMethodName, retType.getUnboxedType(propDef.propType));
            }
            if (retType.isPrimitive()) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, retType.boxedClass, retType.boxerFn, retType.getBoxerDescriptor(), false);
            }
            mv.visitInsn(Opcodes.AASTORE);
        }

        //    ALOAD 2
        //    ARETURN
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 2 + propDefList.size());

        cw.visitEnd();

        return new ClassDef(dotClassName, cw.toByteArray());
    }


    //    public Object[] readVals(Object pojo) {
    //        Object[] res = new Object[3];
    //        Source3 src = ((Source3)pojo);
    //        int counter = 0;
    //        res[counter++] = src.getA1();
    //        res[counter++] = src.getA2();
    //        res[counter++] = src.getA3();
    //        return res;
    //    }
    private static <T> ClassDef generateMultiWriterClass(Class<T> pojoClass, List<PropDef> propDefList, String nameSuffix) {
        String slashClassName = Conf.genReaderSlashClassName(nameSuffix);
        String dotClassName = Conf.genReaderDotClassName(nameSuffix);


        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(Conf.JAVA_VERSION,         // Java version
            Opcodes.ACC_PUBLIC,             // public class
            slashClassName,                 // package and name
            null,                           // signature (null means not generic)
            "java/lang/Object",             // superclass
            new String[]{Conf.MULTIWRITER_INTERFACE}); // interfaces

        // Build constructor
        //
        MethodVisitor con = cw.visitMethod(
            Opcodes.ACC_PUBLIC,                 // public method
            "<init>",                           // method name
            "()V",                              // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        con.visitCode();                        // Start the code for this method
        con.visitVarInsn(Opcodes.ALOAD, 0);  // Load "this" onto the stack

        con.visitMethodInsn(Opcodes.INVOKESPECIAL,  // Invoke an instance method (non-virtual)
            "java/lang/Object",                 // Class on which the method is defined
            "<init>",                           // Name of the method
            "()V",                              // Descriptor
            false);                             // Is this class an interface?

        con.visitInsn(Opcodes.RETURN);          // End the constructor method
        con.visitMaxs(1, 1);            // Specify max stack and local vars

        // Build 'writeVals' method
        //
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,                 // public method
            "writeVals",                          // name
            "(Ljava/lang/Object;[Ljava/lang/Object;)V", // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        mv.visitCode();

        //    ALOAD 1
        //    CHECKCAST package1/Sample1
        //    ASTORE 3
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(pojoClass));
        mv.visitVarInsn(Opcodes.ASTORE, 3);

        //    ALOAD 3
        //    ALOAD 2
        //    ICONST_0
        //    AALOAD
        //    CHECKCAST java/lang/String
        //    INVOKEVIRTUAL package1/Sample1.setA1 (Ljava/lang/String;)V
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.AALOAD);

        int i = 0;
        for (PropDef propDef : propDefList) {
            //    ALOAD 3
            //    ALOAD 2
            //    BIPUSH 6
            //    AALOAD
            //    CHECKCAST java/lang/Integer
            //    INVOKEVIRTUAL java/lang/Integer.intValue ()I -- for primitives
            //    INVOKEVIRTUAL package1/Sample1.setA2 (I)V
            RetType retType = getRetTypeDes(propDef.propType);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            putInt(mv, i++);
            mv.visitInsn(Opcodes.AALOAD);
            mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(propDef.propType));
            if (retType.isPrimitive()) {
                //    INVOKEVIRTUAL java/lang/Integer.intValue ()I
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, retType.boxedClass, retType.unboxerFn, retType.getUnboxerDescriptor(), false);
            }
            if (propDef.isAccessor) {
                //    INVOKEVIRTUAL package1/Sample1.setA2 (I)V
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Conf.classSlashName(pojoClass), propDef.fieldOrMethodName, "(" + retType.getUnboxedType(propDef.propType) + ")V", false);
            } else { // field
                // PUTFIELD package1/ExampleWriter1$IntVal.intValue : I
                mv.visitFieldInsn(Opcodes.PUTFIELD, Conf.classSlashName(pojoClass), propDef.fieldOrMethodName, retType.getUnboxedType(propDef.propType));
            }
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(3, 1 + propDefList.size());

        cw.visitEnd();

        return new ClassDef(dotClassName, cw.toByteArray());
    }

    private static void putInt(MethodVisitor mv, int value) {
        switch (value) {
            case -1: mv.visitInsn(Opcodes.ICONST_M1); return;
            case 0: mv.visitInsn(Opcodes.ICONST_0); return;
            case 1: mv.visitInsn(Opcodes.ICONST_1); return;
            case 2: mv.visitInsn(Opcodes.ICONST_2); return;
            case 3: mv.visitInsn(Opcodes.ICONST_3); return;
            case 4: mv.visitInsn(Opcodes.ICONST_4); return;
            case 5: mv.visitInsn(Opcodes.ICONST_5); return;
        }

        // bipush between -128 and 127.
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
            return;
        }
        // sipush is the same except that it stores a 16 bit constant instead of an 8 bit constant
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
            return;
        }
        mv.visitLdcInsn(value);
    }

    static RetType getRetTypeDes(Class<?> type) {
        if (!type.isPrimitive())
            return new RetType("Ljava/lang/Object;", Opcodes.ALOAD, Opcodes.ARETURN, null, null);
        if (type.equals(int.class))
            return new RetType("I", Opcodes.ILOAD, Opcodes.IRETURN, "Integer", "intValue");
        if (type.equals(long.class))
            return new RetType("J", Opcodes.LLOAD, Opcodes.LRETURN, "Long", "longValue");
        if (type.equals(boolean.class))
            return new RetType("Z", Opcodes.ILOAD, Opcodes.IRETURN, "Boolean", "booleanValue");
        if (type.equals(short.class))
            return new RetType("S", Opcodes.ILOAD, Opcodes.IRETURN, "Short", "shortValue");
        if (type.equals(byte.class))
            return new RetType("B", Opcodes.ILOAD, Opcodes.IRETURN, "Byte", "byteValue");
        if (type.equals(char.class))
            return new RetType("C", Opcodes.ILOAD, Opcodes.IRETURN, "Character", "charValue");
        if (type.equals(double.class))
            return new RetType("D", Opcodes.DLOAD, Opcodes.DRETURN, "Double", "doubleValue");
        if (type.equals(float.class))
            return new RetType("F", Opcodes.FLOAD, Opcodes.FRETURN, "Float", "floatValue");
        // "[" for array - not supported
        throw new IllegalStateException("Type " + type.getName() + " not supported yet");
    }

    private static Field getField(Class<?> pojoClass, String fieldName) {
        Field field;
        try {
            field = pojoClass.getField(fieldName);
            if (field.isSynthetic())
                return null;
            if (Modifier.isStatic(field.getModifiers()))
                return null;
            if (!Modifier.isPublic(field.getModifiers()))
                return null;
        } catch (NoSuchFieldException e) {
            return null;
        }
        return field;
    }

    private static Method getGetter(Class<?> pojoClass, String fieldName) {
        String capitalizedName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String getterName = "get" + capitalizedName;
        Method getter = null;
        try {
            getter = pojoClass.getDeclaredMethod(getterName);
        } catch (NoSuchMethodException e) {
            // look for more
        }

        String booleanName = "is" + capitalizedName;
        if (getter == null) {
            try {
                getter = pojoClass.getDeclaredMethod(booleanName);
                if (!getter.getReturnType().equals(Boolean.class) && !getter.getReturnType().equals(boolean.class))
                    return null;
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        if (Modifier.isStatic(getter.getModifiers()))
            return null;
        if (getter.getParameterCount() > 0)
            return null;

        return getter;
    }

    private static Method getSetter(Class<?> pojoClass, String fieldName) {
        String capitalizedName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String setterName = "set" + capitalizedName;
        return Arrays.stream(pojoClass.getDeclaredMethods())
            .filter(m -> m.getName().equals(setterName)
                && m.getParameterCount() == 1
                && !Modifier.isStatic(m.getModifiers()))
            .findFirst()
            .orElse(null);
    }
}

/* ********************************************************************
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
}
