package com.lostkingdoms.db.exceptions;

public class MissingOrganizedEntityKeyException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1711787121497466152L;
	
	/**
	 * The reason for this exception
	 */
	private String reason;
	
	public MissingOrganizedEntityKeyException(String reason) {
		this.reason = reason;
	}
	
	public String toString() {
		return "MissingOrganizedEntityKeyException: " + "Every OrganizedEntity needs a key in its annotation! Class: " + reason;
	}
	
}
