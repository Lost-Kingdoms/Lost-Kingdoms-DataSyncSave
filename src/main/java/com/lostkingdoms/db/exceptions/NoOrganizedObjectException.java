package com.lostkingdoms.db.exceptions;

import java.lang.reflect.Field;

public class NoOrganizedObjectException extends Exception {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8679885105374318361L;

	/**
	 * The wrong field
	 */
	private final transient Field field;
	
	/**
	 * The class in which the wrong field is in
	 */
	private final Class<?> buildClass;
	
	public NoOrganizedObjectException(Field field, Class<?> buildClass) {
		this.field = field;
		this.buildClass = buildClass;
	}

	@Override
	public String toString() {
		return "NoOrganizedObjectException: Field" + field.getName() + " in build class " + buildClass.getSimpleName() + " has no OrganizedObject annotation!";
	}
	
}
