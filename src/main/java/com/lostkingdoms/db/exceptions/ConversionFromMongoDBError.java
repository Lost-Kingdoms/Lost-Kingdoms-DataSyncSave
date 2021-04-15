package com.lostkingdoms.db.exceptions;

public class ConversionFromMongoDBError extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 442110602011522636L;

	/**
	 * The reason for this exception
	 */
	private final String reason;
	
	public ConversionFromMongoDBError(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return "ConversionFromMongoDBError: MongoDB data String \"" + reason + "\" could not be converted";
	}
	
}
