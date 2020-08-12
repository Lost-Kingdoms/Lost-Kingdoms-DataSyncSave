package com.lostkingdoms.db.exceptions;

public class IllegalIdentifierClassException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7098350761847826205L;
	
	/**
	 * The reason for this exception
	 */
	private String reason;
	
	public IllegalIdentifierClassException(String reason) {
		this.reason = reason;
	}
	
	public String toString() {
		return "IllegalIdentifierClassException: " + reason + " instead of UUID";
	}
}
