package com.github.labai.utils.convert;

/**
 * @author Augustus
 * created on 2022.11.19
 */
public class LaConvertException extends RuntimeException {
    public LaConvertException(String message) {
        super(message);
    }

    public LaConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}
