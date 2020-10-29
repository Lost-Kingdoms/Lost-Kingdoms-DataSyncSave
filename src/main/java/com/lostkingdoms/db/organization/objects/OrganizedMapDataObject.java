package com.lostkingdoms.db.organization.objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultListDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultMapDataConverter;
import com.lostkingdoms.db.factories.JedisFactory;
import com.lostkingdoms.db.factories.MongoDBFactory;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;
import com.lostkingdoms.db.sync.DataSyncMessage;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import redis.clients.jedis.Jedis;

/**
 * The base class for data which should be saved or synced as a map.
 * Provides basic map functions. Later it is planned to fully implement {@link Map} interface.
 *
 * @param <K, V> the classes of the data that should be synced
 */
public final class OrganizedMapDataObject<K, V> extends OrganizedDataObject<HashMap<K, V>> {

	/**
	 * The {@link DefaultMapDataConverter} that will be used for serialization and
	 * deserialization.
	 */
	private DefaultMapDataConverter<K, V> converter;
	
	/**
	 * Constructor for {@link OrganizedMapDataObject}.
	 * This represent a map of objects that should be organized.
	 * 
	 * @param dataKey The objects {@link DataKey}
	 * @param organizationType The objects {@link OrganizationType}
	 */
	public OrganizedMapDataObject(DataKey dataKey, OrganizationType organizationType, DefaultMapDataConverter<K, V> converter) {
		setDataKey(dataKey);
		this.converter = converter;
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
		
		if(mongodb == null || jedis == null) {
			return Collections.unmodifiableMap(getData());
		}
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			short hashslot = getDataKey().getHashslot();
			//TODO
			hashslot = 101;
			
			// If data is up-to-date
			if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() && getTimestamp() != 0) {
				return Collections.unmodifiableMap(getData());
			}
			
			// Data is not up-to-date or null
			// Try to get data from redis global cache
			String dataString = null;
			if(jedis != null) dataString = jedis.get(getDataKey().getRedisKey());
			
			// Check if data is null
			if(dataString != null) {
				//Get the converter to convert the data 
				DefaultMapDataConverter<K, V> converter = this.converter;

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

			if(mongodb != null) {
				DBCollection collection = mongodb.getCollection(dataKey.getMongoDBCollection());
				BasicDBObject query = new BasicDBObject();
				query.put("identifier", dataKey.getMongoDBIdentifier());

				DBObject object = collection.findOne(query);
				if(object != null) {
					dataString = (String) object.get(dataKey.getMongoDBValue());
				}
			}
			
			//Check if data is null
			if(dataString != null) {
				//Get the converter to convert the data 
				DefaultMapDataConverter<K, V> converter = this.converter;

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
				if(jedis != null) jedis.set(dataKey.getRedisKey(), converter.convertToDatabase(getData()));

				return Collections.unmodifiableMap(getData());
			}

			//Data does not exist yet
			return getData();
		} finally {
			if(jedis != null) jedis.close();
		}
	}
	
	public void setMap(HashMap<K, V> map) {
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			//Update the timestamp for last change
			updateTimestamp(newTimestamp);
			
			//Get the data key
			DataKey dataKey = getDataKey();
			
			//Get the data converter
			DefaultMapDataConverter<K, V> converter = this.converter;
			
			//Conversion to redis and mongoDB
			String dataString = converter.convertToDatabase(map);
			if(dataString == null) {
				return;
			}
			
			//Update to redis
			if((getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) && jedis != null) {
				if(dataString.equals("")) {
					jedis.del(dataKey.getRedisKey());
				} else {
					jedis.set(dataKey.getRedisKey(), dataString);
				}
			}
			
			//Update to MongoDB
			if((getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) && mongoDB != null) {	
				DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());
				
				//Test if object already exists
				BasicDBObject query = new BasicDBObject();
				query.put("identifier", dataKey.getMongoDBIdentifier());
				
				DBObject object = collection.findOne(query);
				if(object != null) {
					query = new BasicDBObject();
					query.put("identifier", dataKey.getMongoDBIdentifier());

					BasicDBObject newDoc = new BasicDBObject();
					newDoc.put(dataKey.getMongoDBValue(), dataString);
					
					BasicDBObject update = new BasicDBObject();
					update.put("$set", newDoc);
					
					collection.update(query, update);
				}  else {
					BasicDBObject create = new BasicDBObject();
					create.put("identifier", dataKey.getMongoDBIdentifier());
					create.put(dataKey.getMongoDBValue(), dataString);
					
					collection.insert(create);
				}
			}
			
			//Publish to other servers via redis
			if((getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) && jedis != null) {
				jedis.publish(DataOrganizationManager.SYNC_MESSAGE_CHANNEL.getBytes(), new DataSyncMessage(DataOrganizationManager.getInstance().getInstanceID(), dataKey.getHashslot()).serialize());
			}
			
			//Set the local data
			setData(map);
		} finally {
			if(jedis != null) jedis.close();
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
		//TODO
		hashslot = 101;
		
		if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
			setData(new HashMap<K, V>(getMap()));
		}
		
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		//Updated list (clone)
		@SuppressWarnings("unchecked")
		HashMap<K, V> temp = (HashMap<K, V>) getData().clone();
		temp.remove(key);
		temp.put(key, value);
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			//Update the timestamp for last change
			updateTimestamp(newTimestamp);
			
			//Get the data key
			DataKey dataKey = getDataKey();
			
			//Get the data converter
			DefaultMapDataConverter<K, V> converter = this.converter;
			
			//Conversion to redis and mongoDB
			String dataString = converter.convertToDatabase(temp);
			if(dataString == null) {
				return;
			}
			
			//Update to redis
			if((getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) && jedis != null) {
				if(dataString.equals("")) {
					jedis.del(dataKey.getRedisKey());
				} else {
					jedis.set(dataKey.getRedisKey(), dataString);
				}
			}
			
			//Update to MongoDB
			if((getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) && mongoDB != null) {	
				DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());
				
				//Test if object already exists
				BasicDBObject query = new BasicDBObject();
				query.put("identifier", dataKey.getMongoDBIdentifier());
				
				DBObject object = collection.findOne(query);
				if(object != null) {
					query = new BasicDBObject();
					query.put("identifier", dataKey.getMongoDBIdentifier());

					BasicDBObject newDoc = new BasicDBObject();
					newDoc.put(dataKey.getMongoDBValue(), dataString);
					
					BasicDBObject update = new BasicDBObject();
					update.put("$set", newDoc);
					
					collection.update(query, update);
				}  else {
					BasicDBObject create = new BasicDBObject();
					create.put("identifier", dataKey.getMongoDBIdentifier());
					create.put(dataKey.getMongoDBValue(), dataString);
					
					collection.insert(create);
				}
			}
			
			//Publish to other servers via redis
			if((getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) && jedis != null) {
				jedis.publish(DataOrganizationManager.SYNC_MESSAGE_CHANNEL.getBytes(), new DataSyncMessage(DataOrganizationManager.getInstance().getInstanceID(), dataKey.getHashslot()).serialize());
			}
			
			//Set the local data
			setData(temp);
		} finally {
			if(jedis != null) jedis.close();
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
			setData(new HashMap<K, V>(getMap()));
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
				DefaultMapDataConverter<K, V> converter = this.converter;
				
				//Conversion to redis and mongoDB
				String dataString = converter.convertToDatabase(temp);
				if(dataString == null) {
					return;
				}
				
				//Update to redis
				if((getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) && jedis != null) {
					if(dataString.equals("")) {
						jedis.del(dataKey.getRedisKey());
					} else {
						jedis.set(dataKey.getRedisKey(), dataString);
					}
				}
				
				//Update to MongoDB
				if((getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) && mongoDB != null) {	
					DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());
					
					//Test if object already exists
					BasicDBObject query = new BasicDBObject();
					query.put("identifier", dataKey.getMongoDBIdentifier());
					
					DBObject object = collection.findOne(query);
					if(object != null) {
						query = new BasicDBObject();
						query.put("identifier", dataKey.getMongoDBIdentifier());

						BasicDBObject newDoc = new BasicDBObject();
						newDoc.put(dataKey.getMongoDBValue(), dataString);
						
						BasicDBObject update = new BasicDBObject();
						update.put("$set", newDoc);
						
						collection.update(query, update);
					}  else {
						BasicDBObject create = new BasicDBObject();
						create.put("identifier", dataKey.getMongoDBIdentifier());
						create.put(dataKey.getMongoDBValue(), dataString);
						
						collection.insert(create);
					}
				}
				
				//Publish to other servers via redis
				if((getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) && jedis != null) {
					jedis.publish(DataOrganizationManager.SYNC_MESSAGE_CHANNEL.getBytes(), new DataSyncMessage(DataOrganizationManager.getInstance().getInstanceID(), dataKey.getHashslot()).serialize());
				}
				
				//Set the local data
				setData(temp);
			} finally {
				if(jedis != null) jedis.close();
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
			if(jedis != null) jedis.del(dataKey.getRedisKey());
			
			//Delete from MongoDB
			if((getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) && mongoDB != null) {	
				DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());
				
				BasicDBObject query = new BasicDBObject();
				query.put("identifier", new ObjectId(dataKey.getMongoDBIdentifier()));

				BasicDBObject newDoc = new BasicDBObject();
				newDoc.put(dataKey.getMongoDBValue(), "");
				
				BasicDBObject update = new BasicDBObject();
				update.put("$set", newDoc);
				
				collection.update(query, update);
			}
			
			//Publish to other servers via redis
			if((getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) && jedis != null) {
				jedis.publish(DataOrganizationManager.SYNC_MESSAGE_CHANNEL.getBytes(), new DataSyncMessage(DataOrganizationManager.getInstance().getInstanceID(), dataKey.getHashslot()).serialize());
			}
			
			//Set the local data
			setData(new HashMap<K, V>());
		} finally {
			if(jedis != null) jedis.close();
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
