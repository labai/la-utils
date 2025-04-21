package com.github.labai.utils.convert;

import org.jetbrains.annotations.Nullable;

/**
 * @author Augustus
 * created on 2022.11.19
 */
public interface ITypeConverter<Fr, To> {
    @Nullable To convert(@Nullable Fr from);
}
