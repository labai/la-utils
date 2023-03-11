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

import com.github.labai.shaded.asm.ClassWriter;
import com.github.labai.shaded.asm.MethodVisitor;
import com.github.labai.shaded.asm.Opcodes;
import com.github.labai.utils.hardreflect.LaHardReflect.NameOrAccessor;
import com.github.labai.utils.hardreflect.PropMultiReader;
import com.github.labai.utils.hardreflect.PropMultiWriter;
import com.github.labai.utils.hardreflect.PropReader;
import com.github.labai.utils.hardreflect.PropWriter;
import com.github.labai.utils.hardreflect.impl.Utils.ClassDef;
import com.github.labai.utils.hardreflect.impl.Utils.AccessDef;
import com.github.labai.utils.hardreflect.impl.Utils.RetType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/* ********************************************************************
 * LaHardReflectImpl
 *
 * private
 *
*/
public final class LaHardReflectImpl {

    static final boolean GETTER = true;
    static final boolean SETTER = false;

    public static <T> PropReader<T> createReaderClass(Class<T> pojoClass, String fieldName) {
        Field field = Utils.getField(pojoClass, fieldName);

        String suffix = Conf.makeSuffix(pojoClass.getSimpleName(), fieldName);

        ClassDef def;
        if (field != null) {
            def = generateReaderClass(pojoClass, fieldName, field.getType(), suffix, false);
        } else {
            Method getter = Utils.getGetter(pojoClass, fieldName);
            if (getter == null)
                return null;
            def = generateReaderClass(pojoClass, getter.getName(), getter.getReturnType(), suffix, true);
        }

        return ClassLoadUtils.getPropReader(def.body, def.name);
    }

    public static <T> PropReader<T> createReaderClass(Class<T> pojoClass, Method getter) {
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

    public static <T> PropWriter<T> createWriterClass(Class<T> pojoClass, String fieldName) {
        Field field = Utils.getField(pojoClass, fieldName);

        String suffix = Conf.makeSuffix(pojoClass.getSimpleName(), fieldName);

        ClassDef def;
        if (field != null) {
            def = generateWriterClass(pojoClass, fieldName, field.getType(), suffix, false);
        } else {
            Method setter = Utils.getSetter(pojoClass, fieldName);
            if (setter == null)
                return null;
            def = generateWriterClass(pojoClass, setter.getName(), setter.getParameterTypes()[0], suffix, true);
        }
        return ClassLoadUtils.getPropWriter(def.body, def.name);
    }

    public static <T> PropWriter<T> createWriterClass(Class<T> pojoClass, Method setter) {
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
    public static PropMultiReader createMultiReaderClass(Class<?> pojoClass, List<NameOrAccessor> props) {
        List<AccessDef> propDefs = props.stream()
            .map(it -> Utils.convToFieldOrAccessor(pojoClass, it, GETTER))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        String suffix = Conf.makeSuffixMulti(pojoClass.getSimpleName(), props.size());
        ClassDef def = generateMultiReaderClass(pojoClass, propDefs, suffix);

        return ClassLoadUtils.getPropMultiReader(def.body, def.name);
    }

    public static PropMultiWriter createMultiWriterClass(Class<?> pojoClass, List<NameOrAccessor> props) {
        List<AccessDef> propDefs = props.stream()
            .map(it -> Utils.convToFieldOrAccessor(pojoClass, it, SETTER))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        String suffix = Conf.makeSuffixMulti(pojoClass.getSimpleName(), props.size());
        ClassDef def = generateMultiWriterClass(pojoClass, propDefs, suffix);

        return ClassLoadUtils.getPropMultiWriter(def.body, def.name);
    }

    private static <T> ClassDef generateReaderClass(Class<T> pojoClass, String fieldOrMethodName, Class<?> propType, String nameSuffix, boolean isGetter) {
        String slashClassName = Conf.genReaderSlashClassName(nameSuffix);
        String dotClassName = Conf.genReaderDotClassName(nameSuffix);

        RetType retType = Utils.getRetTypeDes(propType);

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

        RetType retType = Utils.getRetTypeDes(propType);

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

    //    public Object[] readVals(Object pojo) {
    //        Object[] res = new Object[3];
    //        Source3 src = ((Source3)pojo);
    //        int counter = 0;
    //        res[counter++] = src.getA1();
    //        res[counter++] = src.getA2();
    //        res[counter++] = src.getA3();
    //        return res;
    //    }
    private static <T> ClassDef generateMultiReaderClass(Class<T> pojoClass, List<AccessDef> propDefList, String nameSuffix) {
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

        Utils.putInt(mv, propDefList.size());
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

        for (AccessDef propDef : propDefList) {
            //    ALOAD 2
            //    ILOAD 4
            //    IINC 4 1
            //    ALOAD 3
            //    INVOKEVIRTUAL package1/Source3.getA1 ()Ljava/lang/String;
            //    AASTORE
            RetType retType = Utils.getRetTypeDes(propDef.propType);
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
    private static <T> ClassDef generateMultiWriterClass(Class<T> pojoClass, List<AccessDef> propDefList, String nameSuffix) {
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
        for (AccessDef propDef : propDefList) {
            //    ALOAD 3
            //    ALOAD 2
            //    BIPUSH 6
            //    AALOAD
            //    CHECKCAST java/lang/Integer
            //    INVOKEVIRTUAL java/lang/Integer.intValue ()I -- for primitives
            //    INVOKEVIRTUAL package1/Sample1.setA2 (I)V
            RetType retType = Utils.getRetTypeDes(propDef.propType);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            Utils.putInt(mv, i++);
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
}
