package com.github.labai.utils.convert;

/**
 * @author Augustus
 * created on 2022.11.19
 */
public interface IConverterResolver {
    <Fr, To> ITypeConverter<Fr, To> getConverter(Class<Fr> sourceType, Class<To> targetType);
}
