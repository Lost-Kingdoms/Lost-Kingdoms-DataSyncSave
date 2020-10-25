package com.lostkingdoms.db.exceptions;

public class WrongIdentifierException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6927176803810817015L;

	/**
	 * The reason for this exception
	 */
	private Class<?> reason;
	
	/**
	 * The reason for this exception
	 */
	private Class<?> identifier;
	
	public WrongIdentifierException(Class<?> clazz, Class<?> identifier) {
		this.reason = clazz;
		this.identifier = identifier;
	}
	
	public String toString() {
		return "WrongIdentifierException: Class" + identifier.getSimpleName() + " is not identifier class of class " + reason.getSimpleName() + "!";
	}
	
}
