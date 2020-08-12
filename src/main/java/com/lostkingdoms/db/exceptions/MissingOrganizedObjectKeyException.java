package com.lostkingdoms.db.exceptions;

public class MissingOrganizedObjectKeyException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1668105296716648289L;
	
	/**
	 * The reason for this exception
	 */
	private String reason;
	
	public MissingOrganizedObjectKeyException(String reason) {
		this.reason = reason;
	}
	
	public String toString() {
		return "MissingOrganizedObjectKeyException: " + "Every OrganizedEntity needs a key in its annotation! Object: " + reason;
	}
	
}
