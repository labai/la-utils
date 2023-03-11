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
import com.github.labai.utils.hardreflect.LaHardCopy.PojoArgDef;
import com.github.labai.utils.hardreflect.LaHardCopy.PojoCopier;
import com.github.labai.utils.hardreflect.LaHardCopy.PojoCopyPropDef;
import com.github.labai.utils.hardreflect.impl.Utils.AccessDef;
import com.github.labai.utils.hardreflect.impl.Utils.ArgDef;
import com.github.labai.utils.hardreflect.impl.Utils.ClassDef;
import com.github.labai.utils.hardreflect.impl.Utils.RetType;
import com.github.labai.shaded.asm.ClassWriter;
import com.github.labai.shaded.asm.MethodVisitor;
import com.github.labai.shaded.asm.Opcodes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * @author Augustus
 * created on 2023.02.27
 *
 * for internal usage only!
 */
public final class LaHardCopyImpl {

    private static class Pair<F, S> {
        public final F first;
        public final S second;
        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <Fr, To> PojoCopier createPojoCopierClass(
        Class<Fr> pojoFrClass,
        Class<To> pojoToClass,
        List<PojoArgDef> argDefs,
        List<PojoCopyPropDef> propCopyDefs
    ) {

        List<Pair<ArgDef, AccessDef>> argPairs = argDefs.stream()
            .map(it -> {
                    AccessDef adef = Utils.convToFieldOrAccessor(pojoFrClass, it, LaHardReflectImpl.GETTER);
                    if (adef == null)
                        return null;
                    else
                        return new Pair<>(new ArgDef(it.argType), adef);
                }
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<Pair<AccessDef, AccessDef>> propPairs = propCopyDefs.stream()
            .map(it -> {
                AccessDef reader = Utils.convToFieldOrAccessor(pojoFrClass, it, LaHardReflectImpl.GETTER);
                AccessDef writer = Utils.convToFieldOrAccessor(pojoToClass, it, LaHardReflectImpl.SETTER);
                if (reader == null || writer == null) {
                    return null;
                } else {
                    if (reader.dataSupplier != null && reader.propType == null && reader.convFn == null) { // dataSupplier must return target field type
                        AccessDef targ = Utils.convToFieldOrAccessor(pojoToClass, it.target, false);
                        if (targ != null)
                            reader.propType = targ.propType;
                    }
                    return new Pair<>(reader, writer);
                }

            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // get converters (map: convFn -> positionInArray)
        Set<ITypeConverter> convFnSet = new HashSet<>();
        convFnSet.addAll(argPairs.stream().map(it -> it.second.convFn).filter(Objects::nonNull).collect(Collectors.toSet()));
        convFnSet.addAll(propPairs.stream().map(it -> it.second.convFn).filter(Objects::nonNull).collect(Collectors.toSet()));
        final int[] iPos = {0};
        Map<ITypeConverter, Integer> convFnMap = convFnSet.stream().collect(Collectors.toMap(it -> it, it -> iPos[0]++));
        ITypeConverter[] convMapArr = new ITypeConverter[convFnMap.size()];
        for (Entry<ITypeConverter, Integer> entry : convFnMap.entrySet()) {
            convMapArr[entry.getValue()] = entry.getKey();
        }

        // get data suppliers (map: Fn -> positionInArray)
        Set<Function<Fr, ?>> supplierFnSet = new HashSet<>();
        supplierFnSet.addAll(argPairs.stream().map(it -> (Function<Fr, ?>) it.second.dataSupplier).filter(Objects::nonNull).collect(Collectors.toSet()));
        supplierFnSet.addAll(propPairs.stream().map(it -> (Function<Fr, ?>) it.first.dataSupplier).filter(Objects::nonNull).collect(Collectors.toSet()));
        final int[] iPos2 = {0};
        Map<Function<Fr, ?>, Integer> supplierFnMap = supplierFnSet.stream().collect(Collectors.toMap(it -> it, it -> iPos2[0]++));
        Function<Fr, ?>[] supplierMapArr = new Function[supplierFnMap.size()];
        for (Entry<Function<Fr, ?>, Integer> entry : supplierFnMap.entrySet()) {
            supplierMapArr[entry.getValue()] = entry.getKey();
        }

        String suffix = "G" + Conf.counter.incrementAndGet();
        ClassDef cdef = generateCopierClass(pojoFrClass, pojoToClass, argPairs, propPairs, convFnMap, supplierFnMap, suffix);

        return ClassLoadUtils.getPojoCopier(cdef.body, cdef.name, convMapArr, supplierMapArr);
    }

    private static <Fr, To> ClassDef generateCopierClass(
        Class<Fr> pojoFrClass,
        Class<To> pojoToClass,
        List<Pair<ArgDef, AccessDef>> argDefList,
        List<Pair<AccessDef, AccessDef>> propDefList,
        Map<ITypeConverter, Integer> convFnMap,
        Map<Function<Fr, ?>, Integer> supplierFnMap,
        String nameSuffix
    ) {
        String slashClassName = Conf.genSlashClassName(Conf.POJOCOPIER_IMPL_BASE + "$" + nameSuffix);
        String dotClassName = Conf.genDotClassName(Conf.POJOCOPIER_IMPL_BASE + "$" + nameSuffix);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(Conf.JAVA_VERSION,         // Java version
            Opcodes.ACC_PUBLIC,             // public class
            slashClassName,                 // package and name
            null,                           // signature (null means not generic)
            Conf.POJOCOPIER_PARENT,         // superclass
            new String[]{}); // interfaces

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
            Conf.POJOCOPIER_PARENT,                 // Class on which the method is defined
            "<init>",                           // Name of the method
            "()V",                              // Descriptor
            false);                             // Is this class an interface?

        con.visitInsn(Opcodes.RETURN);          // End the constructor method
        con.visitMaxs(1, 1);            // Specify max stack and local vars

        // Build 'copyPojo' method
        //
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,                 // public method
            "copyPojo",                          // name
            "(L" + Conf.classSlashName(pojoFrClass) + ";)L" + Conf.classSlashName(pojoToClass) + ";", // descriptor
            null,                               // signature (null means not generic)
            null);                              // exceptions (array of strings)

        mv.visitCode();

        //    NEW package1/Pojo2
        //    DUP
        mv.visitTypeInsn(Opcodes.NEW, Conf.classSlashName(pojoToClass));
        mv.visitInsn(Opcodes.DUP);

        String conDes = "";
        for (Pair<ArgDef, AccessDef> pair : argDefList) {
            ArgDef argDef = pair.first;
            AccessDef argSourceDef = pair.second;
            Integer convPos = null;
            if (argSourceDef.convFn != null) {
                convPos = convFnMap.get(argSourceDef.convFn);
            }
            if (convPos != null) {
                //    ALOAD 0
                //    GETFIELD package1/Copier3.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
                //    ICONST_0 // int - array pos
                //    AALOAD
                mv.visitVarInsn(Opcodes.ALOAD, 0); // this
                mv.visitFieldInsn(Opcodes.GETFIELD, slashClassName, "convArr", "[Lcom/github/labai/utils/convert/ITypeConverter;");
                Utils.putInt(mv, convPos);
                mv.visitInsn(Opcodes.AALOAD);
            }
            Utils.putReader(mv, argSourceDef, pojoFrClass, supplierFnMap, slashClassName);
            RetType argType = Utils.getRetTypeDes(argDef.clazz);
            if (convPos != null) {
                //    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
                //    CHECKCAST java/lang/String
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "com/github/labai/utils/convert/ITypeConverter",
                    "convert", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(argDef.clazz)); // boxed/unboxed??
            }

            if (argType.isPrimitive()) {
                //    INVOKEVIRTUAL java/lang/Integer.intValue ()I
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, argType.boxedClass, argType.unboxerFn, argType.getUnboxerDescriptor(), false);
            }
            conDes += argType.getUnboxedType(argDef.clazz);
        }

        //    INVOKESPECIAL package1/Pojo2.<init> (Ljava/lang/String;Ljava/lang/String;I)V
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Conf.classSlashName(pojoToClass), "<init>", "(" + conDes + ")V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        for (Pair<AccessDef, AccessDef> propPairs : propDefList) {
            AccessDef srcPropDef = propPairs.first;
            AccessDef trgPropDef = propPairs.second;
            //    ALOAD 2
            //    ALOAD 0
            //    GETFIELD package1/Copier2.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
            //    ICONST_3
            //    AALOAD
            //    ALOAD 1
            //    INVOKEVIRTUAL package1/Pojo1.getA04 ()Ljava/lang/String;
            //    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
            //    CHECKCAST java/lang/String
            //    INVOKEVIRTUAL package1/Pojo2.setA04 (Ljava/lang/String;)V
            Integer convPos = null;
            if (trgPropDef.convFn != null) {
                convPos = convFnMap.get(trgPropDef.convFn);
            }
            mv.visitVarInsn(Opcodes.ALOAD, 2); // to
            if (convPos != null) {
                mv.visitVarInsn(Opcodes.ALOAD, 0); // this
                mv.visitFieldInsn(Opcodes.GETFIELD, slashClassName, "convArr", "[Lcom/github/labai/utils/convert/ITypeConverter;");
                Utils.putInt(mv, convPos);
                mv.visitInsn(Opcodes.AALOAD);
            }
            Utils.putReader(mv, srcPropDef, pojoFrClass, supplierFnMap, slashClassName);
            if (convPos != null) {
                //    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
                //    CHECKCAST java/lang/String
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "com/github/labai/utils/convert/ITypeConverter",
                    "convert", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                mv.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(trgPropDef.propType));
            }
            RetType retTypeTo = Utils.getRetTypeDes(trgPropDef.propType);
            Utils.putWriter(mv, trgPropDef, retTypeTo, pojoToClass);
        }

        //    ALOAD 2
        //    ARETURN
        mv.visitVarInsn(Opcodes.ALOAD, 2); // to
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3 + argDefList.size(), 3);


        // build synthetic method (version for recall main method)
        //
        MethodVisitor mv2 = cw.visitMethod(
            Opcodes.ACC_PUBLIC + Opcodes.ACC_SYNTHETIC + Opcodes.ACC_BRIDGE,
            "copyPojo",
            "(Ljava/lang/Object;)Ljava/lang/Object;",
            null,
            null);
        mv2.visitCode();
        mv2.visitVarInsn(Opcodes.ALOAD, 0);
        mv2.visitVarInsn(Opcodes.ALOAD, 1);
        mv2.visitTypeInsn(Opcodes.CHECKCAST, Conf.classSlashName(pojoFrClass));
        mv2.visitMethodInsn(Opcodes.INVOKEVIRTUAL, slashClassName, "copyPojo", "(L" + Conf.classSlashName(pojoFrClass) + ";)L" + Conf.classSlashName(pojoToClass) + ";", false);
        mv2.visitInsn(Opcodes.ARETURN);
        mv2.visitMaxs(2, 2);

        cw.visitEnd();

        return new ClassDef(dotClassName, cw.toByteArray());
    }
}
