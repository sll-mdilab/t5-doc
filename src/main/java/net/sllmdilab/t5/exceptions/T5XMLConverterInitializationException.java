package net.sllmdilab.t5.exceptions;

import net.sllmdilab.commons.exceptions.T5Exception;

public class T5XMLConverterInitializationException extends T5Exception {

	private static final long serialVersionUID = 1L;

	public T5XMLConverterInitializationException() {
	}

	public T5XMLConverterInitializationException(String message) {
		super(message);
	}

	public T5XMLConverterInitializationException(Throwable cause) {
		super(cause);
	}

	public T5XMLConverterInitializationException(String message, Throwable cause) {
		super(message, cause);
	}

	public T5XMLConverterInitializationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
