package com.lostkingdoms.db.organization.objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.database.JedisFactory;
import com.lostkingdoms.db.database.MongoDBFactory;
import com.lostkingdoms.db.organization.DataOrganizationManager;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;
import com.lostkingdoms.db.sync.DataSyncMessage;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import redis.clients.jedis.Jedis;

/**
 * The base class for data which should be saved or synced as a map
 *
 * @param <K, V> the classes of the data that should be synced
 */
public final class OrganizedMapDataObject<K, V> extends OrganizedDataObject<HashMap<K, V>> {

	/**
	 * Constructor for {@link OrganizedMapDataObject}.
	 * This represent a map of objects that should be organized.
	 * 
	 * @param dataKey The objects {@link DataKey}
	 * @param organizationType The objects {@link OrganizationType}
	 */
	public OrganizedMapDataObject(DataKey dataKey, OrganizationType organizationType, DefaultDataConverter<HashMap<K, V>> converter) {
		setDataKey(dataKey);
		setDataConverter(converter);
		setOrganizationType(organizationType);
		setData(new HashMap<K, V>());
	}
	
	/**
	 * Get the {@link Map}
	 * 
	 * @return An unmodifiable instance of the {@link Map}
	 */
	public Map<K, V> getMap() {
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongodb = MongoDBFactory.getInstance().getMongoDatabase();
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			short hashslot = getDataKey().getHashslot();
			
			// If data is up-to-date
			if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() && getTimestamp() != 0) {
				return Collections.unmodifiableMap(getData());
			}
			
			// Data is not up-to-date or null
			// Try to get data from redis global cache
			String dataString = jedis.get(getDataKey().getRedisKey());
			
			// Check if data is null
			if(dataString != null) {
				//Get the converter to convert the data 
				AbstractDataConverter<HashMap<K, V>> converter = getDataConverter();

				//Convert the data
				HashMap<K, V> newData = converter.convertFromDatabase(dataString);

				//Conversion failed
				if(newData == null) {
					return Collections.unmodifiableMap(new HashMap<K, V>());
				}

				//Set the new data
				setData(newData);

				//Update timestamp to indicate when data was last updated
				updateTimestamp(newTimestamp);

				return Collections.unmodifiableMap(getData());
			}
			
			// Data in global cache is null
			// Try to get data from MongoDB
			DataKey dataKey = getDataKey();

			DBCollection collection = mongodb.getCollection(dataKey.getMongoDBCollection());
			BasicDBObject query = new BasicDBObject();
			query.put("uuid", new ObjectId(dataKey.getMongoDBIdentifier()));

			DBObject object = collection.findOne(query);
			dataString = (String) object.get(dataKey.getMongoDBValue());

			//Check if data is null
			if(dataString != null) {
				//Get the converter to convert the data 
				AbstractDataConverter<HashMap<K, V>> converter = getDataConverter();

				//Convert the data
				HashMap<K, V> newData = converter.convertFromDatabase(dataString);

				//Conversion failed
				if(newData == null) {
					return Collections.unmodifiableMap(new HashMap<K, V>());
				}

				//Set the new data
				setData(newData);

				//Update timestamp to indicate when data was last updated
				updateTimestamp(newTimestamp);

				//Push data to Redis
				jedis.set(dataKey.getRedisKey(), converter.convertToDatabase(getData()));

				return Collections.unmodifiableMap(getData());
			}

			//Data does not exist yet
			return getData();
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * Put a key-value pair to the {@link Map}
	 * 
	 * @param key
	 * @param value
	 */
	public void put(K key, V value) {
		short hashslot = getDataKey().getHashslot();
		
		if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
			setData((HashMap<K, V>) getMap());
		}
		
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		//Updated list (clone)
		@SuppressWarnings("unchecked")
		HashMap<K, V> temp = (HashMap<K, V>) getData().clone();
		temp.put(key, value);
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			//Update the timestamp for last change
			updateTimestamp(newTimestamp);
			
			//Get the data key
			DataKey dataKey = getDataKey();
			
			//Get the data converter
			AbstractDataConverter<HashMap<K, V>> converter = getDataConverter();
			
			//Conversion to redis and mongoDB
			String dataString = converter.convertToDatabase(temp);
			if(dataString == null) {
				return;
			}
			
			//Update to redis
			if(dataString.equals("")) {
				jedis.del(dataKey.getRedisKey());
			} else {
				jedis.set(dataKey.getRedisKey(), dataString);
			}
			
			//Update to MongoDB
			if(getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) {	
				DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());

				BasicDBObject query = new BasicDBObject();
				query.put("uuid", new ObjectId(dataKey.getMongoDBIdentifier()));

				BasicDBObject newDoc = new BasicDBObject();
				newDoc.put(dataKey.getMongoDBValue(), dataString);
				
				BasicDBObject update = new BasicDBObject();
				update.put("$set", newDoc);
				
				collection.update(query, update);
			}
			
			//Publish to other servers via redis
			if(getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) {
				jedis.publish(DataOrganizationManager.SYNC_MESSAGE_CHANNEL.getBytes(), new DataSyncMessage(DataOrganizationManager.getInstance().getInstanceID(), dataKey.getHashslot()).serialize());
			}
			
			//Set the local data
			setData(temp);
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * Remove the key from the {@link Map}
	 * 
	 * @param key
	 */
	public void remove(K key) {
		short hashslot = getDataKey().getHashslot();
		
		if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
			setData((HashMap<K, V>) getMap());
		}
		
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		//Updated list (clone)
		@SuppressWarnings("unchecked")
		HashMap<K, V> temp = (HashMap<K, V>) getData().clone();
		Object change = temp.remove(key);
		
		if(change != null) {
			try {
				long newTimestamp = System.currentTimeMillis() - 1;
				
				//Update the timestamp for last change
				updateTimestamp(newTimestamp);
				
				//Get the data key
				DataKey dataKey = getDataKey();
				
				//Get the data converter
				AbstractDataConverter<HashMap<K, V>> converter = getDataConverter();
				
				//Conversion to redis and mongoDB
				String dataString = converter.convertToDatabase(temp);
				if(dataString == null) {
					return;
				}
				
				//Update to redis
				if(dataString.equals("")) {
					jedis.del(dataKey.getRedisKey());
				} else {
					jedis.set(dataKey.getRedisKey(), dataString);
				}
				
				//Update to MongoDB
				if(getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) {	
					DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());

					BasicDBObject query = new BasicDBObject();
					query.put("uuid", new ObjectId(dataKey.getMongoDBIdentifier()));

					BasicDBObject newDoc = new BasicDBObject();
					newDoc.put(dataKey.getMongoDBValue(), dataString);
					
					BasicDBObject update = new BasicDBObject();
					update.put("$set", newDoc);
					
					collection.update(query, update);
				}
				
				//Publish to other servers via redis
				if(getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) {
					jedis.publish(DataOrganizationManager.SYNC_MESSAGE_CHANNEL.getBytes(), new DataSyncMessage(DataOrganizationManager.getInstance().getInstanceID(), dataKey.getHashslot()).serialize());
				}
				
				//Set the local data
				setData(temp);
			} finally {
				jedis.close();
			}
		}
	}
	
	/**
	 * Clear the {@link Map}
	 */
	public void clear() {
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			//Update the timestamp for last change
			updateTimestamp(newTimestamp);
			
			//Get the data key
			DataKey dataKey = getDataKey();
			
			//Delete from Redis
			jedis.del(dataKey.getRedisKey());
			
			//Delete from MongoDB
			if(getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) {	
				DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());
				
				BasicDBObject query = new BasicDBObject();
				query.put("uuid", new ObjectId(dataKey.getMongoDBIdentifier()));

				BasicDBObject newDoc = new BasicDBObject();
				newDoc.put(dataKey.getMongoDBValue(), "");
				
				BasicDBObject update = new BasicDBObject();
				update.put("$set", newDoc);
				
				collection.update(query, update);
			}
			
			//Publish to other servers via redis
			if(getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) {
				jedis.publish(DataOrganizationManager.SYNC_MESSAGE_CHANNEL.getBytes(), new DataSyncMessage(DataOrganizationManager.getInstance().getInstanceID(), dataKey.getHashslot()).serialize());
			}
			
			//Set the local data
			setData(new HashMap<K, V>());
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * Get a value by key from {@link Map}
	 * 
	 * @param key
	 * @return the value V or null if key does not exist
	 */
	public V get(K key) {
		Map<K, V> map = getMap();
		
		return map.get(key);
	}
	
	
	
	/**
	 * Check if {@link Map} contains a key
	 * 
	 * @param key
	 * @return 
	 */
	public boolean containsKey(K key) {
		Map<K, V> map = getMap();
		
		if(map.containsKey(key)) return true;
		return false;
	}
	
	/**
	 * Check if {@link Map} contains a value
	 * 
	 * @param value
	 * @return
	 */
	public boolean containsValue(V value) {
		Map<K, V> map = getMap();
		
		if(map.containsValue(value)) return true;
		return false;
	}
	
	/**
	 * Get the size of the {@link Map}
	 * 
	 * @return The size of the {@link Map}
	 */
	public int size() {
		Map<K, V> map = getMap();
		
		return map.size();
	}
	
	
	
	
	
	
	
	
}
