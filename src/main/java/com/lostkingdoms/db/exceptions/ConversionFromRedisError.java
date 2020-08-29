package com.lostkingdoms.db.exceptions;

public class ConversionFromRedisError extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5658133433428869684L;

	/**
	 * The reason for this exception
	 */
	private String reason;
	
	public ConversionFromRedisError(String reason) {
		this.reason = reason;
	}
	
	public String toString() {
		return "ConversionFromRedisError: Redis data String \"" + reason + "\" could not be converted";
	}
	
}
