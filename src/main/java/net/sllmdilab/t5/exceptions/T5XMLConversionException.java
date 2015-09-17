package net.sllmdilab.t5.exceptions;

import net.sllmdilab.commons.exceptions.T5Exception;

public class T5XMLConversionException extends T5Exception {

	private static final long serialVersionUID = 1L;

	public T5XMLConversionException() {
	}

	public T5XMLConversionException(String message) {
		super(message);
	}

	public T5XMLConversionException(Throwable cause) {
		super(cause);
	}

	public T5XMLConversionException(String message, Throwable cause) {
		super(message, cause);
	}

	public T5XMLConversionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
