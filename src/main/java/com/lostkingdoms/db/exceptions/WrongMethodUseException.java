package com.lostkingdoms.db.exceptions;

public class WrongMethodUseException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2019859171738843504L;

	/**
	 * The method name which was misused
	 */
	private String methodName;
	
	/**
	 * The class which misused the method
	 */
	private Class<?> objectClass;
	
	public WrongMethodUseException(String methodName, Class<?> objectClass) {
		this.methodName = methodName;
		this.objectClass = objectClass;
	}
	
	public String toString() {
		return "WrongMethodUseException: Method " + methodName + " was used for class " + objectClass.getSimpleName();
	}
	
}
