package package1;

import com.github.labai.utils.convert.ITypeConverter;
import com.github.labai.utils.hardreflect.LaHardCopy.AbstractPojoCopier;

import java.util.function.Function;

/**
 * @author Augustus
 * created on 2023.03.05
 */
public class Copier1 extends AbstractPojoCopier<Pojo1, Pojo2> {
    @Override
    public Pojo2 copyPojo(Pojo1 from) {
        Pojo2 target = new Pojo2(from.getA01(), from.getA02(), 1111111111);
        target.setA03(from.getA03());
        target.setA04(from.getA04());
        return target;
    }

    @Override
    public void setTypeConverters(ITypeConverter[] typeConverters) {
    }

    @Override
    public void setDataSuppliers(Function<Pojo1, ?>[] dataSuppliers) {

    }
}

// // class version 52.0 (52)
//// access flags 0x21
//// signature Ljava/lang/Object;Lcom/github/labai/utils/hardreflect/LaHardCopy$PojoCopier<Lpackage1/Pojo1;Lpackage1/Pojo2;>;
//// declaration: package1/Copier1 implements com.github.labai.utils.hardreflect.LaHardCopy$PojoCopier<package1.Pojo1, package1.Pojo2>
//public class package1/Copier1 implements com/github/labai/utils/hardreflect/LaHardCopy$PojoCopier {
//
//  // compiled from: Copier1.java
//  // access flags 0x609
//  public static abstract INNERCLASS com/github/labai/utils/hardreflect/LaHardCopy$PojoCopier com/github/labai/utils/hardreflect/LaHardCopy PojoCopier
//
//  // access flags 0x1
//  public <init>()V
//   L0
//    LINENUMBER 9 L0
//    ALOAD 0
//    INVOKESPECIAL java/lang/Object.<init> ()V
//    RETURN
//   L1
//    LOCALVARIABLE this Lpackage1/Copier1; L0 L1 0
//    MAXSTACK = 1
//    MAXLOCALS = 1
//
//  // access flags 0x1
//  public copyPojo(Lpackage1/Pojo1;)Lpackage1/Pojo2;
//   L0
//    LINENUMBER 13 L0
//    NEW package1/Pojo2
//    DUP
//    ALOAD 1
//    INVOKEVIRTUAL package1/Pojo1.getA01 ()Ljava/lang/String;
//    ALOAD 1
//    INVOKEVIRTUAL package1/Pojo1.getA02 ()Ljava/lang/String;
//    LDC 1111111111
//    INVOKESPECIAL package1/Pojo2.<init> (Ljava/lang/String;Ljava/lang/String;I)V
//    ASTORE 2
//   L1
//    LINENUMBER 14 L1
//    ALOAD 2
//    ALOAD 1
//    INVOKEVIRTUAL package1/Pojo1.getA03 ()Ljava/lang/String;
//    INVOKEVIRTUAL package1/Pojo2.setA03 (Ljava/lang/String;)V
//   L2
//    LINENUMBER 15 L2
//    ALOAD 2
//    ALOAD 1
//    INVOKEVIRTUAL package1/Pojo1.getA04 ()Ljava/lang/String;
//    INVOKEVIRTUAL package1/Pojo2.setA04 (Ljava/lang/String;)V
//   L3
//    LINENUMBER 16 L3
//    ALOAD 2
//    ARETURN
//   L4
//    LOCALVARIABLE this Lpackage1/Copier1; L0 L4 0
//    LOCALVARIABLE from Lpackage1/Pojo1; L0 L4 1
//    LOCALVARIABLE target Lpackage1/Pojo2; L1 L4 2
//    MAXSTACK = 5
//    MAXLOCALS = 3
//
//  // access flags 0x1041
//  public synthetic bridge copyPojo(Ljava/lang/Object;)Ljava/lang/Object;
//   L0
//    LINENUMBER 9 L0
//    ALOAD 0
//    ALOAD 1
//    CHECKCAST package1/Pojo1
//    INVOKEVIRTUAL package1/Copier1.copyPojo (Lpackage1/Pojo1;)Lpackage1/Pojo2;
//    ARETURN
//   L1
//    LOCALVARIABLE this Lpackage1/Copier1; L0 L1 0
//    MAXSTACK = 2
//    MAXLOCALS = 2
//}
