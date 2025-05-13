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

import com.github.labai.shaded.asm.MethodVisitor;
import com.github.labai.shaded.asm.Opcodes;
import com.github.labai.utils.convert.ITypeConverter;
import com.github.labai.utils.hardreflect.LaHardCopy.PojoArgDef;
import com.github.labai.utils.hardreflect.LaHardCopy.PojoCopyPropDef;
import com.github.labai.utils.hardreflect.LaHardReflect.NameOrAccessor;
import com.github.labai.utils.hardreflect.impl.Utils.AccessDef.Kind;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/*
 * @author Augustus
 * created on 2023.02.27
 *
 * various utils
 *
 * for internal usage
 */
final class Utils {

    static class ClassDef {
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

    static class ArgDef {
        final Class<?> clazz;

        public ArgDef(Class<?> clazz) {
            this.clazz = clazz;
        }
    }

    static class AccessDef {
        final Kind kind;
        final Field field;
        final Method accessor;
        final String fieldOrMethodName;
        final boolean isAccessor; // getter/setter (i.e. not field)
        final boolean isGetter; // getter or setter

        Class<?> propType;

        final Object constant;
        final Function<?, ?> dataSupplier;

        final ITypeConverter<?, ?> convFn;

        enum Kind {FIELD, ACCESSOR, SUPPLIER, CONSTANT}


        private AccessDef(Kind kind, Object constant) {
            if (kind != Kind.CONSTANT)
                throw new IllegalStateException("constructor is for constant");
            this.kind = Kind.CONSTANT;
            this.field = null;
            this.accessor = null;
            this.isAccessor = false;
            this.fieldOrMethodName = null;
            this.isGetter = false;
            this.propType = constant == null ? null : constant.getClass();
            this.constant = constant;
            this.dataSupplier = null;
            this.convFn = null;
        }

        private AccessDef(Function<?, ?> dataSupplier, Class<?> resultType) {
            if (dataSupplier == null)
                throw new IllegalStateException("dataSupplier is null");
            this.kind = Kind.SUPPLIER;
            this.field = null;
            this.accessor = null;
            this.isAccessor = false;
            this.fieldOrMethodName = null;
            this.isGetter = true;
            this.propType = resultType;
            this.constant = null;
            this.dataSupplier = dataSupplier;
            this.convFn = null;
        }

        private AccessDef(Method accessor) {
            if (accessor == null)
                throw new IllegalStateException("accessor is null");
            this.kind = Kind.ACCESSOR;
            this.field = null;
            this.accessor = accessor;
            this.isAccessor = true;
            this.fieldOrMethodName = accessor.getName();
            this.isGetter = accessor.getParameterTypes().length == 0;
            this.propType = (isGetter ? accessor.getReturnType() : accessor.getParameterTypes()[0]);
            this.constant = null;
            this.dataSupplier = null;
            this.convFn = null;
        }

        private AccessDef(Field field) {
            if (field == null)
                throw new IllegalStateException("field is null");
            this.kind = Kind.FIELD;
            this.field = field;
            this.accessor = null;
            this.isAccessor = false;
            this.fieldOrMethodName = field.getName();
            this.isGetter = false; // not used for field
            this.propType = field.getType();
            this.constant = null;
            this.dataSupplier = null;
            this.convFn = null;
        }

        private AccessDef(Kind kind, Field field, Method accessor, String fieldOrMethodName, Class<?> propType,
                boolean isAccessor, boolean isGetter, Object constant, ITypeConverter<?, ?> convFn, Function<?, ?> dataSupplier
        ) {
            this.field = field;
            this.accessor = accessor;
            this.fieldOrMethodName = fieldOrMethodName;
            this.propType = propType;
            this.isAccessor = isAccessor;
            this.isGetter = isGetter;
            this.constant = constant;
            this.convFn = convFn;
            this.dataSupplier = dataSupplier;
            this.kind = kind;

        }

        AccessDef withConv(ITypeConverter<?, ?> convFn) {
            return new AccessDef(
                this.kind,
                this.field,
                this.accessor,
                this.fieldOrMethodName,
                this.propType,
                this.isAccessor,
                this.isGetter,
                this.constant,
                convFn,
                this.dataSupplier
            );
        }

        static AccessDef field(Field field) {
            return new AccessDef(field);
        }

        static AccessDef accessor(Method accessor) {
            return new AccessDef(accessor);
        }

        static AccessDef constant(Object constant) {
            return new AccessDef(Kind.CONSTANT, constant);
        }

        static AccessDef dataSupplier(Function<?, ?> dataSupplier, Class<?> resultType) {
            return new AccessDef(dataSupplier, resultType);
        }
    }

    static <T> AccessDef convToFieldOrAccessor(Class<T> pojoClass, PojoArgDef argDef, boolean needGetter) {
        AccessDef res;
        if (argDef.source != null) {
            res = convToFieldOrAccessor(pojoClass, argDef.source, needGetter);
        } else if (argDef.dataSupplier != null) {
            res = AccessDef.dataSupplier(argDef.dataSupplier, argDef.argType);
        } else { // constant may be null
            res = AccessDef.constant(argDef.constant);
        }
        if (res != null && argDef.convFn != null)
            res = res.withConv(argDef.convFn);
        return res;
    }

    static <T> AccessDef convToFieldOrAccessor(Class<T> pojoClass, PojoCopyPropDef propDef, boolean needGetter) {
        AccessDef res;
        if (needGetter && propDef.source != null) {
            res = convToFieldOrAccessor(pojoClass, propDef.source, needGetter);
        } else if (!needGetter && propDef.target != null) {
            res = convToFieldOrAccessor(pojoClass, propDef.target, needGetter);
        } else if (propDef.dataSupplier != null) {
            res = AccessDef.dataSupplier(propDef.dataSupplier, null);
        } else {
            throw new IllegalStateException("PojoCopyPropDef is null");
        }
        if (res != null && propDef.convFn != null)
            res = res.withConv(propDef.convFn);
        return res;
    }

    static <T> AccessDef convToFieldOrAccessor(Class<T> pojoClass, NameOrAccessor prop, boolean needGetter) {
        if (prop.accessor != null)
            return AccessDef.accessor(prop.accessor);
        if (prop.name == null)
            return null;
        Field field = Utils.getField(pojoClass, prop.name);
        if (field != null)
            return AccessDef.field(field);
        if (needGetter) {
            Method getter = Utils.getGetter(pojoClass, prop.name);
            if (getter != null)
                return AccessDef.accessor(getter);
        } else {
            Method setter = Utils.getSetter(pojoClass, prop.name);
            if (setter != null)
                return AccessDef.accessor(setter);

        }
        return null;
    }

    static void putInt(MethodVisitor mv, int value) {
        switch (value) {
            case -1:
                mv.visitInsn(Opcodes.ICONST_M1);
                return;
            case 0:
                mv.visitInsn(Opcodes.ICONST_0);
                return;
            case 1:
                mv.visitInsn(Opcodes.ICONST_1);
                return;
            case 2:
                mv.visitInsn(Opcodes.ICONST_2);
                return;
            case 3:
                mv.visitInsn(Opcodes.ICONST_3);
                return;
            case 4:
                mv.visitInsn(Opcodes.ICONST_4);
                return;
            case 5:
                mv.visitInsn(Opcodes.ICONST_5);
                return;
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

    static <Fr> void putReader(MethodVisitor mv, AccessDef accessDef, Class<?> pojoClass, Map<Function<Fr, ?>, Integer> supplierFnMap, String slashClassName) {
        if (accessDef.accessor != null || accessDef.field != null) {
            RetType retType = Utils.getRetTypeDes(accessDef.propType);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            if (accessDef.isAccessor) {
                //    ALOAD 1
                //    INVOKEVIRTUAL package1/Pojo1.getA01 ()Ljava/lang/String;
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Conf.classSlashName(pojoClass), accessDef.fieldOrMethodName, "()" + retType.getUnboxedType(accessDef.propType), false);
            } else { // field
                mv.visitFieldInsn(Opcodes.GETFIELD, Conf.classSlashName(pojoClass), accessDef.fieldOrMethodName, retType.getUnboxedType(accessDef.propType));
            }
            if (retType.isPrimitive()) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, retType.boxedClass, retType.boxerFn, retType.getBoxerDescriptor(), false);
            }
            return;
        }

        if (accessDef.dataSupplier != null) {
            //    ALOAD 0
            //    GETFIELD package1/Copier5.dataSupplierArr : [Ljava/util/function/Function;
            //    ICONST_0 // pos in arr
            //    AALOAD
            //    ALOAD 1
            //    INVOKEINTERFACE java/util/function/Function.apply (Ljava/lang/Object;)Ljava/lang/Object; (itf)
            //    CHECKCAST java/lang/String
            Integer fnPos = supplierFnMap.get(accessDef.dataSupplier);
            if (fnPos == null)
                throw new IllegalStateException("dataSupplier not found");
            mv.visitVarInsn(Opcodes.ALOAD, 0); // this
            mv.visitFieldInsn(Opcodes.GETFIELD, slashClassName, "dataSupplierArr", "[Ljava/util/function/Function;");
            Utils.putInt(mv, fnPos);
            mv.visitInsn(Opcodes.AALOAD);
            mv.visitVarInsn(Opcodes.ALOAD, 1); // fr
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/function/Function", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", true); // input type?
            if (accessDef.convFn == null && accessDef.propType != null) { // if exist converter then it will checkcast
                mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(accessDef.propType));
            }
            return;
        }

        if (accessDef.kind == Kind.CONSTANT) {
            if (accessDef.constant == null) {
                mv.visitInsn(Opcodes.ACONST_NULL);
            } else {
                // todo now support int only (used for kotlin constructor)
                if (accessDef.constant instanceof Integer) {
                    Utils.putInt(mv, (Integer) accessDef.constant);
                    RetType intType = Utils.getRetTypeDes(int.class);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, intType.boxedClass, intType.boxerFn, intType.getBoxerDescriptor(), false);
                }
            }
            return;
        }

    }

    static void putWriter(MethodVisitor mv, AccessDef accessDef, RetType retType, Class<?> pojoClass) {
        if (retType.isPrimitive()) {
            //    INVOKEVIRTUAL java/lang/Integer.intValue ()I
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, retType.boxedClass, retType.unboxerFn, retType.getUnboxerDescriptor(), false);
        }
        if (accessDef.isAccessor) {
            //    INVOKEVIRTUAL package1/Sample1.setA2 (I)V
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Conf.classSlashName(pojoClass), accessDef.fieldOrMethodName, "(" + retType.getUnboxedType(accessDef.propType) + ")V", false);
        } else { // field
            // PUTFIELD package1/ExampleWriter1$IntVal.intValue : I
            mv.visitFieldInsn(Opcodes.PUTFIELD, Conf.classSlashName(pojoClass), accessDef.fieldOrMethodName, retType.getUnboxedType(accessDef.propType));
        }
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

    static Field getField(Class<?> pojoClass, String fieldName) {
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

    static Method getGetter(Class<?> pojoClass, String fieldName) {
        Method getter = null;

        // with "get" prefix - getField()
        String capitalizedName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String getterName = "get" + capitalizedName;
        try {
            getter = pojoClass.getDeclaredMethod(getterName);
        } catch (NoSuchMethodException e) {
            // look for more
        }

        // w/o prefix - field()
        if (getter == null) {
            try {
                getter = pojoClass.getDeclaredMethod(fieldName);
            } catch (NoSuchMethodException e) {
                // look for more
            }
        }

        // with "is" prefix - isField() - for booleans only
        if (getter == null) {
            String booleanName = "is" + capitalizedName;
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

    static Method getSetter(Class<?> pojoClass, String fieldName) {
        // with "set" prefix - setField(value)
        String capitalizedName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String setterName = "set" + capitalizedName;
        Method setter = Arrays.stream(pojoClass.getDeclaredMethods())
            .filter(m -> m.getName().equals(setterName)
                && m.getParameterCount() == 1
                && !Modifier.isStatic(m.getModifiers()))
            .findFirst()
            .orElse(null);
        if (setter == null) {
            // w/o prefix - field(value)
            setter = Arrays.stream(pojoClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(fieldName)
                    && m.getParameterCount() == 1
                    && !Modifier.isStatic(m.getModifiers()))
                .findFirst()
                .orElse(null);
        }
        return setter;
    }
}
