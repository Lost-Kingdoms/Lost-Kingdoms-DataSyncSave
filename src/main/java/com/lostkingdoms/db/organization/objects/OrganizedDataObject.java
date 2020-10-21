package com.lostkingdoms.db.organization.objects;

import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;

public abstract class OrganizedDataObject<T> {

	/**
	 * The time when the list was last updated
	 */
	private long timestamp;
	
	/**
	 * Contains the keys for jedis and mongoDB and the jedis hashslot for this object
	 */
	private DataKey dataKey;
	
	/**
	 * The {@link OrganizationType} how this {@link OrganizedMapDataObject}
	 * should be processed when put or remove method is called.
	 */
	private OrganizationType organizationType;
	
	/**
	 * The {@link AbstractDataConverter} that will be used for serialization and
	 * deserialization.
	 */
	private AbstractDataConverter<T> converter;
	
	/**
	 * THE data map object 
	 */
	private T data;

	
	protected T getData() {
		return this.data;
	}
	
	protected void setData(T data) {
		this.data = data;
	}
	
	/**
	 * Get the {@link DataKey} for this {@link OrganizedSingleDataObject}
	 * 
	 * @return
	 */
	protected DataKey getDataKey() {
		return this.dataKey;
	}
	
	/**
	 * Set the {@link DataKey} for this object. 
	 * Can NOT replace an old {@link DataKey}
	 * 
	 * @param dataKey The {@link DataKey} to be set
	 */
	protected void setDataKey(DataKey dataKey) {
		if(this.dataKey == null) this.dataKey = dataKey;
	}
	
	/**
	 * Get the timestamp of the last update of this {@link OrganizedSingleDataObject}
	 * 
	 * @return the timestamp
	 */
	protected long getTimestamp() {
		return this.timestamp;
	}
	
	/**
	 * Set the last updated timestamp
	 * 
	 * @param The new timestamp
	 */
	protected void updateTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * Set the {@link OrganizationType} for this object. 
	 * Can NOT replace old {@link OrganizationType}
	 * 
	 * @param organizationType The {@link OrganizationType} to be set
	 */
	protected void setOrganizationType(OrganizationType organizationType) {
		if(this.organizationType == null) this.organizationType = organizationType;
	}
	
	/**
	 * Set the {@link IDataConverter} for this object.
	 * Can NOT replace old {@link IDataConverter}
	 * 
	 * @param converter
	 */
	protected void setDataConverter(AbstractDataConverter<T> converter) {
		if(this.converter == null) this.converter = converter;
	}
	
	/**
	 * Get this objects {@link IDataConverter}
	 * 
	 * @return The converter
	 */
	protected AbstractDataConverter<T> getDataConverter() {
		return this.converter;
	}
	
	/**
	 * Get the {@link OrganizationType} of this object
	 * 
	 * @return
	 */
	protected OrganizationType getOrganizationType() {
		return this.organizationType;
	}
	
}
