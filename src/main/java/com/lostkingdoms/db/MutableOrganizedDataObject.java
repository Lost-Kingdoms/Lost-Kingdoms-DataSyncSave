package com.lostkingdoms.db;

public class MutableOrganizedDataObject<T> extends OrganizedDataObject<T> {

	/**
	 * The {@link OrganizationType} how this {@link MutableOrganizedDataObject}
	 * should be processed when set method is called.
	 */
	private OrganizationType organizationType;
	
	
	
	/**
	 * Constructor for writable {@link OrganizedDataObject}
	 * 
	 * @param dataKey
	 * @param organizationType
	 */
	public MutableOrganizedDataObject(DataKey dataKey, OrganizationType organizationType) {
		super.dataKey = dataKey;
		this.organizationType = organizationType;
	}
	
	/**
	 * The public setter for the data. Processes the data according to 
	 * {@link OrganizationType}.
	 * 
	 * @param data
	 */
	public void set(T data) {
		if(data != null) {
			super.data = data;
			
			// Update the timestamp for last change
			updateTimestamp();
			
			// Trigger an update of this data Object
			DataAccessObject.save(this);
		} else {
			super.data = null;
			
			// Update the timestamp for last change
			updateTimestamp();
			
			// Trigger remove of this data Object
			DataAccessObject.remove(this);
		}
	}
	
}
