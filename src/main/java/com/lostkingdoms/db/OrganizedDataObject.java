package com.lostkingdoms.db;

import com.lostkingdoms.db.jedis.JedisFactory;

import redis.clients.jedis.Jedis;

/**
 * The base class for data which should be saved or synced
 *
 * @param <T> the class of the data that should be synced
 */
public class OrganizedDataObject<T> {

	/**
	 * The time when the value was last updated
	 */
	private long timestamp;
	
	/**
	 * Contains the keys for jedis and mongoDB and the jedis hashslot for this object
	 */
	protected DataKey dataKey;
	
	/**
	 * THE data object 
	 */
	protected T data;
	
	
	
	/**
	 * The public getter method for the data.
	 * Checks consistency of data with global cache and DB.
	 * 
	 * @return the data of this {@link OrganizedDataObject}
	 */
	public T get() {	
		// Update timestamp (-1) so that if data is synced at the same time an update 
		// is triggered.
		
		updateTimestamp();
		
		short hashslot = getDataKey().getHashslot();
		
		// If data is up-to-date
		
		if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp()) {
			return data;
		}
		
		// Data is not up-to-date
		// Try to get data from redis global cache
		
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		String data = jedis.get(getDataKey().getRedisKey());
		
		// Check if data is null
		
		if(data != null) {
			//TODO
		}
		
		// Data in global cache is null
		// Try to get data from MongoDB
		
	}
	
	/**
	 * Get the {@link DataKey} for this {@link OrganizedDataObject}
	 * 
	 * @return
	 */
	protected DataKey getDataKey() {
		return this.dataKey;
	}
	
	/**
	 * Get the timestamp of the last update of this {@link OrganizedDataObject}
	 * 
	 * @return the timestamp
	 */
	protected long getTimestamp() {
		return this.timestamp;
	}
	
	/**
	 * Set the last updated timestamp to current time - 1
	 */
	protected void updateTimestamp() {
		this.timestamp = System.currentTimeMillis()-1;
	}
	
}
