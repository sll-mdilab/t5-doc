package net.sllmdilab.t5.exceptions;

import net.sllmdilab.commons.exceptions.T5Exception;

public class T5ConversionException extends T5Exception {

	private static final long serialVersionUID = 1L;

	public T5ConversionException() {
	}

	public T5ConversionException(String message) {
		super(message);
	}

	public T5ConversionException(Throwable cause) {
		super(cause);
	}

	public T5ConversionException(String message, Throwable cause) {
		super(message, cause);
	}

	public T5ConversionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
