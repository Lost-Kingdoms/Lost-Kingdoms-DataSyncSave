package com.lostkingdoms.db;

/**
 * A read only {@link OrganizedDataObject} of type T
 *
 * @param <T>
 */
public class ImmutableOrganizedDataObject<T> extends OrganizedDataObject<T> {

	/**
	 * Constructor for non writable {@link OrganizedDataObject}
	 * 
	 * @param dataKey
	 */
	public ImmutableOrganizedDataObject(DataKey dataKey) {
		super.dataKey = dataKey;
	}
	
}
