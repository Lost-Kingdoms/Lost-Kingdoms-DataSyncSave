package com.lostkingdoms.db.organization.objects;

import org.bson.types.ObjectId;

import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.database.JedisFactory;
import com.lostkingdoms.db.database.MongoDBFactory;
import com.lostkingdoms.db.logger.LKLogger;
import com.lostkingdoms.db.logger.LogType;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;
import com.lostkingdoms.db.sync.DataSyncMessage;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import redis.clients.jedis.Jedis;

/**
 * The base class for single data which should be saved or synced.
 * (no list, map) 
 *
 * @param <T> the class of the data that should be synced
 */
public final class OrganizedSingleDataObject<T> extends OrganizedDataObject<T> {	
	
	private Object defaultValue = null;
	
	/**
	 * Constructor for {@link OrganizedSingleDataObject}.
	 * This represent a single object that should be organized (no list, map).
	 * 
	 * @param dataKey The objects {@link DataKey}
	 * @param organizationType The objects {@link OrganizationType}
	 */
	public OrganizedSingleDataObject(DataKey dataKey, OrganizationType organizationType, DefaultDataConverter<T> converter) {
		setDataKey(dataKey);
		setDataConverter(converter);
		setOrganizationType(organizationType);
	}
	
	/**
	 * Constructor for {@link OrganizedSingleDataObject}.
	 * This represent a single object that should be organized (no list, map).
	 * 
	 * @param dataKey The objects {@link DataKey}
	 * @param organizationType The objects {@link OrganizationType}
	 * @param converter The converter of this object
	 * @param the default value of this object
	 */
	public OrganizedSingleDataObject(DataKey dataKey, OrganizationType organizationType, DefaultDataConverter<T> converter, Object defaultValue) {
		setDataKey(dataKey);
		setDataConverter(converter);
		setOrganizationType(organizationType);
		this.defaultValue = defaultValue;
	}
	
	/**
	 * The public getter method for the data.
	 * Checks consistency of data with global cache and DB.
	 * 
	 * @return the data of this {@link OrganizedSingleDataObject}
	 */
	public T get() {	
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongodb = MongoDBFactory.getInstance().getMongoDatabase();
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			short hashslot = getDataKey().getHashslot();
			LKLogger.getInstance().debug("SingleDataGet Hashslot: " + hashslot, LogType.GET);
			hashslot = 100;
			
			// If data is up-to-date
			LKLogger.getInstance().debug("SingleDataGet Timestamp: " + DataOrganizationManager.getInstance().getLastUpdated(hashslot) + "   " + getTimestamp(), LogType.GET);
			if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() && getTimestamp() != 0) {
				return getData();
			}
			
			// Data is not up-to-date or null
			// Try to get data from redis global cache
			LKLogger.getInstance().debug("SingleDataGet RedisData: " + getDataKey().getRedisKey() + "   " + jedis.get(getDataKey().getRedisKey()), LogType.GET);
			String dataString = jedis.get(getDataKey().getRedisKey());
			
			// Check if data is null
			if(dataString != null) {
				//Get the converter to convert the data 
				AbstractDataConverter<T> converter = getDataConverter();
				
				//Convert the data
				T newData = converter.convertFromDatabase(dataString);
				
				//Conversion failed
				if(newData == null) {
					return null;
				}
				
				//Set the new data
				setData(newData);
				
				//Update timestamp to indicate when data was last updated
				updateTimestamp(newTimestamp);
				
				return getData();
			}
			
			// Data in global cache is null
			// Try to get data from MongoDB
			DataKey dataKey = getDataKey();
		
			DBCollection collection = mongodb.getCollection(dataKey.getMongoDBCollection());
			BasicDBObject query = new BasicDBObject();
			query.put("uuid", dataKey.getMongoDBIdentifier());
			
			DBObject object = collection.findOne(query);
			if(object != null) {
				dataString = (String) object.get(dataKey.getMongoDBValue());
			} 
			
			
			//Check if data is null
			LKLogger.getInstance().debug("SingleDataGet MongoData: " + dataKey.getMongoDBValue(), LogType.GET);
			if(dataString != null) {
				//Get the converter to convert the data 
				AbstractDataConverter<T> converter = getDataConverter();
				
				//Convert the data
				T newData = converter.convertFromDatabase(dataString);
				
				//Conversion failed
				if(newData == null) {
					return null;
				}
				
				//Set the new data
				setData(newData);
				
				//Update timestamp to indicate when data was last updated
				updateTimestamp(newTimestamp);
				
				//Push data to Redis
				jedis.set(dataKey.getRedisKey(), converter.convertToDatabase(getData()));
				
				return getData();
			}
			
			//Data does not exist yet (check for default value)
			if(defaultValue != null) {
				return (T) defaultValue;
			}
			return null;
		} finally {
			jedis.close();
		}
	}
	
	/**
	 * The public setter for the data. Processes the data according to 
	 * {@link OrganizationType}.
	 * 
	 * @param data
	 */
	public void set(T data) {
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			//Update the timestamp for last change
			updateTimestamp(newTimestamp);
			
			//Get the data key
			DataKey dataKey = getDataKey();
			LKLogger.getInstance().debug("SingleDataSet: DataKey " + dataKey , LogType.ALL);
			
			//Get the data converter
			AbstractDataConverter<T> converter = getDataConverter();
			LKLogger.getInstance().debug("SingleDataSet: DataConverter " + converter , LogType.ALL);
			
			//Conversion to redis and mongoDB
			String dataString = converter.convertToDatabase(data);
			LKLogger.getInstance().debug("SingleDataSet: DataString " + dataString , LogType.ALL);
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
				
				//Test if object already exists
				BasicDBObject query = new BasicDBObject();
				query.put("uuid", dataKey.getMongoDBIdentifier());
				
				DBObject object = collection.findOne(query);
				if(object != null) {
					query = new BasicDBObject();
					query.put("uuid", dataKey.getMongoDBIdentifier());

					BasicDBObject newDoc = new BasicDBObject();
					newDoc.put(dataKey.getMongoDBValue(), dataString);
					
					BasicDBObject update = new BasicDBObject();
					update.put("$set", newDoc);
					
					collection.update(query, update);
				}  else {
					BasicDBObject create = new BasicDBObject();
					create.put("uuid", dataKey.getMongoDBIdentifier());
					create.put(dataKey.getMongoDBValue(), dataString);
					
					collection.insert(create);
				}
			}
			
			//Publish to other servers via redis
			if(getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) {
				jedis.publish(DataOrganizationManager.SYNC_MESSAGE_CHANNEL.getBytes(), new DataSyncMessage(DataOrganizationManager.getInstance().getInstanceID(), dataKey.getHashslot()).serialize());
			}

			//Set the local data
			setData(data);
		} finally {
			jedis.close();
		}
	}
	
	
	
}
