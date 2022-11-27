package com.github.labai.utils.convert;

/**
 * @author Augustus
 * created on 2022.11.19
 */
public interface ITypeConverter<Fr, To> {
    To convert(Fr from);
}
