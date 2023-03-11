package package1;

import com.github.labai.utils.hardreflect.LaHardCopy.AbstractPojoCopier;
import package1.Copier4.Pojo2_1;

/**
 * @author Augustus
 * created on 2023.03.05
 */
public class Copier4 extends AbstractPojoCopier<Pojo1, Pojo2_1> {


    public Copier4() {
    }

    static class Pojo2_1 {
        String a01 = "a01";
        String a02 = "a02";
        String a03 = "a03";
        String a04 = "a04";
        String a05 = "a05";
        String a06 = "a06";
        String a07 = "a07";
        String a08 = "a08";

        public Pojo2_1(String a01, String a02, String a03, String a04) {
            this.a01 = a01;
            this.a02 = a02;
            this.a03 = a03;
            this.a04 = a04;
        }
    }


    @Override
    public Pojo2_1 copyPojo(Pojo1 from) {
        Pojo2_1 target = new Pojo2_1(
            (String) convArr[0].convert(from.getA01()),
            (String) convArr[1].convert(from.getA02()),
            (String) convArr[0].convert(from.getA01()),
            (String) convArr[1].convert(from.getA02())
            );
        target.a05 = (String) convArr[2].convert(from.getA03());
        return target;
    }
}

// // class version 52.0 (52)
//// access flags 0x21
//// signature Lcom/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier<Lpackage1/Pojo1;Lpackage1/Copier4$Pojo2_1;>;
//// declaration: package1/Copier4 extends com.github.labai.utils.hardreflect.LaHardCopy$AbstractPojoCopier<package1.Pojo1, package1.Copier4$Pojo2_1>
//public class package1/Copier4 extends com/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier {
//
//  // compiled from: Copier4.java
//  // access flags 0x409
//  public static abstract INNERCLASS com/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier com/github/labai/utils/hardreflect/LaHardCopy AbstractPojoCopier
//  // access flags 0x8
//  static INNERCLASS package1/Copier4$Pojo2_1 package1/Copier4 Pojo2_1
//
//  // access flags 0x1
//  public <init>()V
//   L0
//    LINENUMBER 13 L0
//    ALOAD 0
//    INVOKESPECIAL com/github/labai/utils/hardreflect/LaHardCopy$AbstractPojoCopier.<init> ()V
//   L1
//    LINENUMBER 14 L1
//    RETURN
//   L2
//    LOCALVARIABLE this Lpackage1/Copier4; L0 L2 0
//    MAXSTACK = 1
//    MAXLOCALS = 1
//
//  // access flags 0x1
//  public copyPojo(Lpackage1/Pojo1;)Lpackage1/Copier4$Pojo2_1;
//   L0
//    LINENUMBER 37 L0
//    NEW package1/Copier4$Pojo2_1
//    DUP
//    ALOAD 0
//    GETFIELD package1/Copier4.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_0
//    AALOAD
//    ALOAD 1
//   L1
//    LINENUMBER 38 L1
//    INVOKEVIRTUAL package1/Pojo1.getA01 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//    ALOAD 0
//    GETFIELD package1/Copier4.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_1
//    AALOAD
//    ALOAD 1
//   L2
//    LINENUMBER 39 L2
//    INVOKEVIRTUAL package1/Pojo1.getA02 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//    ALOAD 0
//    GETFIELD package1/Copier4.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_0
//    AALOAD
//    ALOAD 1
//   L3
//    LINENUMBER 40 L3
//    INVOKEVIRTUAL package1/Pojo1.getA01 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//    ALOAD 0
//    GETFIELD package1/Copier4.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_1
//    AALOAD
//    ALOAD 1
//   L4
//    LINENUMBER 41 L4
//    INVOKEVIRTUAL package1/Pojo1.getA02 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//    INVOKESPECIAL package1/Copier4$Pojo2_1.<init> (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
//    ASTORE 2
//   L5
//    LINENUMBER 43 L5
//    ALOAD 2
//    ALOAD 0
//    GETFIELD package1/Copier4.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_2
//    AALOAD
//    ALOAD 1
//    INVOKEVIRTUAL package1/Pojo1.getA03 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//    PUTFIELD package1/Copier4$Pojo2_1.a05 : Ljava/lang/String;
//   L6
//    LINENUMBER 44 L6
//    ALOAD 2
//    ARETURN
//   L7
//    LOCALVARIABLE this Lpackage1/Copier4; L0 L7 0
//    LOCALVARIABLE from Lpackage1/Pojo1; L0 L7 1
//    LOCALVARIABLE target Lpackage1/Copier4$Pojo2_1; L5 L7 2
//    MAXSTACK = 7
//    MAXLOCALS = 3
//
//  // access flags 0x1041
//  public synthetic bridge copyPojo(Ljava/lang/Object;)Ljava/lang/Object;
//   L0
//    LINENUMBER 10 L0
//    ALOAD 0
//    ALOAD 1
//    CHECKCAST package1/Pojo1
//    INVOKEVIRTUAL package1/Copier4.copyPojo (Lpackage1/Pojo1;)Lpackage1/Copier4$Pojo2_1;
//    ARETURN
//   L1
//    LOCALVARIABLE this Lpackage1/Copier4; L0 L1 0
//    MAXSTACK = 2
//    MAXLOCALS = 2
//}
