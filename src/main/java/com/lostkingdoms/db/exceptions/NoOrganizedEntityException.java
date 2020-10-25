package com.lostkingdoms.db.exceptions;

public class NoOrganizedEntityException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 9139914353921118724L;
	
	/**
	 * The wrong class
	 */
	private Class<?> buildClass;
	
	public NoOrganizedEntityException(Class<?> buildClass) {
		this.buildClass = buildClass;
	}
	
	public String toString() {
		return "NoOrganizedEntityException: Class " + buildClass.getSimpleName() + " has no OrganizedEntity annotation!";
	}
	
}
