package com.lostkingdoms.db.organization.objects;

import org.bson.types.ObjectId;

import com.lostkingdoms.db.converters.IDataConverter;
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
 * The base class for single data which should be saved or synced.
 * (no list, map) 
 *
 * @param <T> the class of the data that should be synced
 */
public class OrganizedSingleDataObject<T> extends OrganizedDataObject<T> {	
	
	/**
	 * Constructor for {@link OrganizedSingleDataObject}.
	 * This represent a single object that should be organized (no list, map).
	 * 
	 * @param dataKey The objects {@link DataKey}
	 * @param organizationType The objects {@link OrganizationType}
	 */
	public OrganizedSingleDataObject(DataKey dataKey, OrganizationType organizationType) {
		setDataKey(dataKey);
		setOrganizationType(organizationType);
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
			
			// If data is up-to-date
			if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() && getTimestamp() != 0) {
				return getData();
			}
			
			// Data is not up-to-date or null
			// Try to get data from redis global cache
			String dataString = jedis.get(getDataKey().getRedisKey());
			
			// Check if data is null
			if(dataString != null) {
				//Get the converter to convert the data 
				IDataConverter<T> converter = getDataConverter();
				
				//Convert the data
				T newData = converter.convertFromRedis(dataString);
				
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
			query.put("uuid", new ObjectId(dataKey.getMongoDBIdentifier()));
			
			DBObject object = collection.findOne(query);
			dataString = (String) object.get(dataKey.getMongoDBValue());
			
			//Check if data is null
			if(dataString != null) {
				//Get the converter to convert the data 
				IDataConverter<T> converter = getDataConverter();
				
				//Convert the data
				T newData = converter.convertFromMongoDB(dataString);
				
				//Conversion failed
				if(newData == null) {
					return null;
				}
				
				//Set the new data
				setData(newData);
				
				//Update timestamp to indicate when data was last updated
				updateTimestamp(newTimestamp);
				
				//Push data to Redis
				jedis.set(dataKey.getRedisKey(), converter.convertToRedis(getData()));
				
				return getData();
			}
			
			//Data does not exist yet
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
			
			//Get the data converter
			IDataConverter<T> converter = getDataConverter();
			
			//Conversion to redis and mongoDB
			String redisDataString = converter.convertToRedis(data);
			String mongoDataString = converter.convertToMongoDB(data);
			if(redisDataString == null || mongoDataString == null) {
				return;
			}

			//Update to redis
			if(redisDataString.equals("")) {
				jedis.del(dataKey.getRedisKey());
			} else {
				jedis.set(dataKey.getRedisKey(), redisDataString);
			}
			
			//Update to MongoDB
			if(getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) {	
				DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());

				BasicDBObject query = new BasicDBObject();
				query.put("uuid", new ObjectId(dataKey.getMongoDBIdentifier()));

				BasicDBObject newDoc = new BasicDBObject();
				newDoc.put(dataKey.getMongoDBValue(), mongoDataString);
				
				BasicDBObject update = new BasicDBObject();
				update.put("$set", newDoc);
				
				collection.update(query, update);
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
