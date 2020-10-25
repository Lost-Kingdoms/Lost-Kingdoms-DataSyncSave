package com.lostkingdoms.db.exceptions;

public class NoIdentifierError extends Error {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6979979813500166135L;

	/**
	 * The reason for this exception
	 */
	private Class<?> reason;
	
	
	public NoIdentifierError(Class<?> clazz) {
		this.reason = clazz;
	}
	
	public String toString() {
		return "NoIdentifierError: Class" + reason.getSimpleName() + " has no specified identifier!";
	}
	
}
