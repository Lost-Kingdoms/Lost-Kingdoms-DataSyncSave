package com.lostkingdoms.db.exceptions;

public class DatabaseObjectConversionException extends Error {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3239958666304672454L;

	/**
	 * The reason for this exception
	 */
	private String reason;
	
	/**
	 * The converter that has failed
	 */
	private String converter;
	
	public DatabaseObjectConversionException(String reason, String converter) {
		this.reason = reason;
		this.converter = converter;
	}
	
	@Override
	public String toString() {
		return "ObjectNotConvertableError: " + "String \"" + reason + "\" could not be converted by " + converter;
	}
	
}
