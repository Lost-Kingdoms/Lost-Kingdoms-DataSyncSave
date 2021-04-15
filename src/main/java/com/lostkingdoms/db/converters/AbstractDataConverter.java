package com.lostkingdoms.db.converters;

/**
 * An abstract DataConverter. Used as template for custom converters
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 * @param <T>
 */
public abstract class AbstractDataConverter<T> {

	private final Class<T> thisClass;
	
	protected AbstractDataConverter(Class<T> thisClass) {
		this.thisClass = thisClass;
	}
	
	/**
	 *Get the class of this converter at runtime
	 * 
	 * @return
	 */
	protected Class<T> getThisClass() {
		return this.thisClass;
	}
	
	/**
	 * Convert a string from MongoDB or redis back to an java object
	 * 
	 * @param s
	 * @return
	 */
	public abstract T convertFromDatabase(String s);
	
	/**
	 * Convert a java object to MongoDB or redis
	 * 
	 * @param o
	 * @return
	 */
	public abstract String convertToDatabase(Object o);
	
	/**
	 * An utility method to create generic list or map class objects
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> castClass(Class<?> clazz) {
        return (Class<T>) clazz;
    }
}
