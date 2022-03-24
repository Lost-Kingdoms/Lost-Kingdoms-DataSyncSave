package com.lostkingdoms.db.organization.objects;

import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;
import com.lostkingdoms.db.sync.DataSyncMessage;
import redis.clients.jedis.Jedis;

/**
 * Abstract class of all OrganizedDataObjects
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 * @param <T>
 */
public abstract class OrganizedDataObject<T> {

	/** The String used in MongoDB for the identifier*/
	protected static final String IDENTIFIER = "_id";
	
	/** The time when the list was last updated */
	private long timestamp;
	
	/** Contains the keys for jedis and mongoDB and the jedis hashslot for this object */
	private DataKey dataKey;
	
	/**
	 * The {@link OrganizationType} how this {@link OrganizedMapDataObject}
	 * should be processed when put or remove method is called.
	 */
	private OrganizationType organizationType;
	
	/** THE data map object */
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
	 * @return the {@link DataKey}
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
	 * @param timestamp The new timestamp
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
	 * Get the {@link OrganizationType} of this object
	 * 
	 * @return the {@link OrganizationType}
	 */
	protected OrganizationType getOrganizationType() {
		return this.organizationType;
	}

	/**
	 * Sends the sync message if {@link OrganizationType} equals SYNC or ALL
	 *
	 * @param jedis the jedis instance to publish on
	 */
	protected void sendSyncMessage(Jedis jedis) {
		if (getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) {
			jedis.publish(DataOrganizationManager.syncMessageChannel,
					new DataSyncMessage(DataOrganizationManager.getInstance().getInstanceID(), dataKey.getHashslot()).serialize());
		}
	}

}
