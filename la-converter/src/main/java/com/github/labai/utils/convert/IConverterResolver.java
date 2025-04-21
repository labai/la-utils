package com.github.labai.utils.convert;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Augustus
 * created on 2022.11.19
 */
public interface IConverterResolver {
    <Fr, To> @Nullable ITypeConverter<Fr, To> getConverter(@NotNull Class<Fr> sourceType, @NotNull Class<To> targetType);
}
