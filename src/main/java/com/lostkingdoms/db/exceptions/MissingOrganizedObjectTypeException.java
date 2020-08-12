package com.lostkingdoms.db.exceptions;

public class MissingOrganizedObjectTypeException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8702245695844682187L;
	
	/**
	 * The reason for this exception
	 */
	private String reason;
	
	public MissingOrganizedObjectTypeException(String reason) {
		this.reason = reason;
	}
	
	public String toString() {
		return "MissingOrganizedObjectTypeException: " + "Every OrganizedObject needs a OrganizationType in its annotation! Object: " + reason;
	}
	
}
