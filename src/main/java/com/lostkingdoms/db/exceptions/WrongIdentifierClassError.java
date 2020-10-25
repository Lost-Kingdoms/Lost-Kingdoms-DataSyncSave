package com.lostkingdoms.db.exceptions;

public class WrongIdentifierClassError extends Error {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4053059425845289899L;

	/**
	 * The reason for this exception
	 */
	private Class<?> reason;
	
	/**
	 * The reason for this exception
	 */
	private Class<?> identifier;
	
	public WrongIdentifierClassError(Class<?> clazz, Class<?> identifier) {
		this.reason = clazz;
		this.identifier = identifier;
	}
	
	public String toString() {
		return "WrongIdentifierClassError: Class" + reason.getSimpleName() + " has identifier class " + identifier.getSimpleName() + ". Only String, UUID or Enum allowed!";
	}
	
}
