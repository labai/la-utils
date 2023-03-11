package package1;

import com.github.labai.utils.hardreflect.LaHardCopy.AbstractPojoCopier;

/**
 * @author Augustus
 * created on 2023.03.07
 */
public class Copier5 extends AbstractPojoCopier<Pojo1, Pojo2> {


    public Copier5() {
    }

    @Override
    public Pojo2 copyPojo(Pojo1 from) {
        Pojo2 target = new Pojo2(
            (String)
            dataSupplierArr[0].apply(from),
            (String)
            convArr[1].convert(
                dataSupplierArr[1].apply(from)
            ),
            1111111111);
        target.setA03(
            (String)
            dataSupplierArr[2].apply(from)
        );
        target.setA04(
            (String)
            convArr[3].convert(
            dataSupplierArr[3].apply(from)
            )
        );
        return target;
    }
}

// // class version 52.0 (52)
//// access flags 0x21
//// signature Lcom/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier<Lpackage1/Pojo1;Lpackage1/Pojo2;>;
//// declaration: package1/Copier5 extends com.github.labai.utils.hardreflect.LaHardCopy$AbstractPojoCopier<package1.Pojo1, package1.Pojo2>
//public class package1/Copier5 extends com/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier {
//
//  // compiled from: Copier5.java
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
//    LINENUMBER 13 L1
//    RETURN
//   L2
//    LOCALVARIABLE this Lpackage1/Copier5; L0 L2 0
//    MAXSTACK = 1
//    MAXLOCALS = 1
//
//  // access flags 0x1
//  public copyPojo(Lpackage1/Pojo1;)Lpackage1/Pojo2;
//   L0
//    LINENUMBER 17 L0
//    NEW package1/Pojo2
//    DUP
//    ALOAD 0
//    GETFIELD package1/Copier5.dataSupplierArr : [Ljava/util/function/Function;
//    ICONST_0
//    AALOAD
//    ALOAD 1
//   L1
//    LINENUMBER 19 L1
//    INVOKEINTERFACE java/util/function/Function.apply (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String

//    ALOAD 0
//    GETFIELD package1/Copier5.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_1
//    AALOAD
//    ALOAD 0
//    GETFIELD package1/Copier5.dataSupplierArr : [Ljava/util/function/Function;
//    ICONST_1
//    AALOAD
//    ALOAD 1
//   L2
//    LINENUMBER 22 L2
//    INVOKEINTERFACE java/util/function/Function.apply (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//   L3
//    LINENUMBER 21 L3
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String

//    LDC 1111111111
//    INVOKESPECIAL package1/Pojo2.<init> (Ljava/lang/String;Ljava/lang/String;I)V
//    ASTORE 2
//   L4
//    LINENUMBER 25 L4

//    ALOAD 2
//    ALOAD 0
//    GETFIELD package1/Copier5.dataSupplierArr : [Ljava/util/function/Function;
//    ICONST_2
//    AALOAD
//    ALOAD 1
//   L5
//    LINENUMBER 27 L5
//    INVOKEINTERFACE java/util/function/Function.apply (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//   L6
//    LINENUMBER 25 L6
//    INVOKEVIRTUAL package1/Pojo2.setA03 (Ljava/lang/String;)V
//   L7
//    LINENUMBER 29 L7

//    ALOAD 2
//    ALOAD 0
//    GETFIELD package1/Copier5.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_3
//    AALOAD
//    ALOAD 0
//    GETFIELD package1/Copier5.dataSupplierArr : [Ljava/util/function/Function;
//    ICONST_3
//    AALOAD
//    ALOAD 1
//   L8
//    LINENUMBER 32 L8
//    INVOKEINTERFACE java/util/function/Function.apply (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//   L9
//    LINENUMBER 31 L9
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//   L10
//    LINENUMBER 29 L10
//    INVOKEVIRTUAL package1/Pojo2.setA04 (Ljava/lang/String;)V
//   L11
//    LINENUMBER 35 L11
//    ALOAD 2
//    ARETURN
//   L12
//    LOCALVARIABLE this Lpackage1/Copier5; L0 L12 0
//    LOCALVARIABLE from Lpackage1/Pojo1; L0 L12 1
//    LOCALVARIABLE target Lpackage1/Pojo2; L4 L12 2
//    MAXSTACK = 6
//    MAXLOCALS = 3
//
//  // access flags 0x1041
//  public synthetic bridge copyPojo(Ljava/lang/Object;)Ljava/lang/Object;
//   L0
//    LINENUMBER 9 L0
//    ALOAD 0
//    ALOAD 1
//    CHECKCAST package1/Pojo1
//    INVOKEVIRTUAL package1/Copier5.copyPojo (Lpackage1/Pojo1;)Lpackage1/Pojo2;
//    ARETURN
//   L1
//    LOCALVARIABLE this Lpackage1/Copier5; L0 L1 0
//    MAXSTACK = 2
//    MAXLOCALS = 2
//}
