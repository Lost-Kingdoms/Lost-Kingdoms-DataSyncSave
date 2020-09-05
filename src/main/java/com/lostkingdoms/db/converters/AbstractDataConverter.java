package com.lostkingdoms.db.converters;

/**
 * An abstract DataConverter
 * 
 * @author Tim
 *
 * @param <T>
 */
public abstract class AbstractDataConverter<T> {

	private Class<T> thisClass;
	
	public AbstractDataConverter(Class<T> thisClass) {
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
	
}
