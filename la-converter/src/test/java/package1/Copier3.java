package package1;

import com.github.labai.utils.hardreflect.LaHardCopy.AbstractPojoCopier;

/**
 * @author Augustus
 * created on 2023.03.05
 */
public class Copier3 extends AbstractPojoCopier<Pojo1, Pojo2> {


    public Copier3() {
    }

    @Override
    public Pojo2 copyPojo(Pojo1 from) {
        Pojo2 target = new Pojo2(
            (String) convArr[0].convert(from.getA01()),
            (String) convArr[1].convert(from.getA02()),
            1111111111);
        target.setA03(
            (String) convArr[2].convert(from.getA03())
        );
        target.setA04(
            (String) convArr[3].convert(from.getA04())
        );
        return target;
    }
}

// // class version 52.0 (52)
//// access flags 0x21
//// signature Lcom/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier<Lpackage1/Pojo1;Lpackage1/Pojo2;>;
//// declaration: package1/Copier3 extends com.github.labai.utils.hardreflect.LaHardCopy$AbstractPojoCopier<package1.Pojo1, package1.Pojo2>
//public class package1/Copier3 extends com/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier {
//
//  // compiled from: Copier3.java
//  // access flags 0x409
//  public static abstract INNERCLASS com/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier com/github/labai/utils/hardreflect/LaHardCopy AbstractPojoCopier
//
//  // access flags 0x1
//  public <init>()V
//   L0
//    LINENUMBER 12 L0
//    ALOAD 0
//    INVOKESPECIAL com/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier.<init> ()V
//   L1
//    LINENUMBER 20 L1
//    RETURN
//   L2
//    LOCALVARIABLE this Lpackage1/Copier3; L0 L2 0
//    MAXSTACK = 1
//    MAXLOCALS = 1
//
//  // access flags 0x1
//  public copyPojo(Lpackage1/Pojo1;)Lpackage1/Pojo2;
//   L0
//    LINENUMBER 24 L0
//    NEW package1/Pojo2
//    DUP

//    ALOAD 0
//    GETFIELD package1/Copier3.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_0
//    AALOAD
//    ALOAD 1
//   L1
//    LINENUMBER 25 L1
//    INVOKEVIRTUAL package1/Pojo1.getA01 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String

//    ALOAD 0
//    GETFIELD package1/Copier3.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_1
//    AALOAD
//    ALOAD 1
//   L2
//    LINENUMBER 26 L2
//    INVOKEVIRTUAL package1/Pojo1.getA02 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String

//    LDC 1111111111
//    INVOKESPECIAL package1/Pojo2.<init> (Ljava/lang/String;Ljava/lang/String;I)V
//    ASTORE 2
//   L3
//    LINENUMBER 28 L3
//    ALOAD 2
//    ALOAD 0
//    GETFIELD package1/Copier3.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_2
//    AALOAD
//    ALOAD 1
//   L4
//    LINENUMBER 29 L4
//    INVOKEVIRTUAL package1/Pojo1.getA03 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//   L5
//    LINENUMBER 28 L5
//    INVOKEVIRTUAL package1/Pojo2.setA03 (Ljava/lang/String;)V
//   L6
//    LINENUMBER 31 L6
//    ALOAD 2
//    ALOAD 0
//    GETFIELD package1/Copier3.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_3
//    AALOAD
//    ALOAD 1
//   L7
//    LINENUMBER 32 L7
//    INVOKEVIRTUAL package1/Pojo1.getA04 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//   L8
//    LINENUMBER 31 L8
//    INVOKEVIRTUAL package1/Pojo2.setA04 (Ljava/lang/String;)V
//   L9
//    LINENUMBER 34 L9
//    ALOAD 2
//    ARETURN
//   L10
//    LOCALVARIABLE this Lpackage1/Copier3; L0 L10 0
//    LOCALVARIABLE from Lpackage1/Pojo1; L0 L10 1
//    LOCALVARIABLE target Lpackage1/Pojo2; L3 L10 2
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
//    INVOKEVIRTUAL package1/Copier3.copyPojo (Lpackage1/Pojo1;)Lpackage1/Pojo2;
//    ARETURN
//   L1
//    LOCALVARIABLE this Lpackage1/Copier3; L0 L1 0
//    MAXSTACK = 2
//    MAXLOCALS = 2
//}
