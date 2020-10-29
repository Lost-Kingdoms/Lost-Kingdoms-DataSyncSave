package com.lostkingdoms.db.organization.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;

import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.converters.impl.DefaultListDataConverter;
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
 * An {@link OrganizedDataObject} which represents an {@link ArrayList} of type T.
 * Provides basic list methods. Later it is planned to fully implement {@link List} interface
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 * @param <T>
 */
public final class OrganizedListDataObject<T> extends OrganizedDataObject<ArrayList<T>> {

	/**
	 * The {@link DefaultListDataConverter} that will be used for serialization and
	 * deserialization.
	 */
	private DefaultListDataConverter<T> converter;
	
	/**
	 * Constructor for {@link OrganizedListDataObject}.
	 * This represent a list of objects that should be organized.
	 * 
	 * @param dataKey The objects {@link DataKey}
	 * @param organizationType The objects {@link OrganizationType}
	 */
	public OrganizedListDataObject(DataKey dataKey, OrganizationType organizationType, DefaultListDataConverter<T> converter) {
		setDataKey(dataKey);
		this.converter = converter;
		setOrganizationType(organizationType);
		setData(new ArrayList<T>());
	}
	
	/**
	 * Get the {@link List}
	 * 
	 * @return An unmodifiable instance of the {@link List}
	 */
	public List<T> getList() {
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongodb = MongoDBFactory.getInstance().getMongoDatabase();
		
		if(mongodb == null || jedis == null) {
			return Collections.unmodifiableList(getData());
		}
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			short hashslot = getDataKey().getHashslot();
			//TODO
			hashslot = 100;
			
			// If data is up-to-date
			if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() && getTimestamp() != 0) {
				return Collections.unmodifiableList(getData());
			}
			
			// Data is not up-to-date or null
			// Try to get data from redis global cache
			String dataString = null;
			if(jedis != null) dataString = jedis.get(getDataKey().getRedisKey());
			
			// Check if data is null
			if(dataString != null) {
				//Get the converter to convert the data 
				DefaultListDataConverter<T> converter = this.converter;

				//Convert the data
				ArrayList<T> newData = converter.convertFromDatabase(dataString);

				//Conversion failed
				if(newData == null) {
					return Collections.unmodifiableList(new ArrayList<T>());
				}

				//Set the new data
				setData(newData);

				//Update timestamp to indicate when data was last updated
				updateTimestamp(newTimestamp);

				return Collections.unmodifiableList(getData());
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
				DefaultListDataConverter<T> converter = this.converter;

				//Convert the data
				ArrayList<T> newData = converter.convertFromDatabase(dataString);

				//Conversion failed
				if(newData == null) {
					return Collections.unmodifiableList(new ArrayList<T>());
				}

				//Set the new data
				setData(newData);

				//Update timestamp to indicate when data was last updated
				updateTimestamp(newTimestamp);

				//Push data to Redis
				if(jedis != null) jedis.set(dataKey.getRedisKey(), converter.convertToDatabase(getData()));

				return Collections.unmodifiableList(getData());
			}

			//Data does not exist yet
			return getData();
		} finally {
			if(jedis != null) jedis.close();
		}
	}
	
	/**
	 * Set an {@link ArrayList}
	 * 
	 * @param list
	 */
	public void setList(ArrayList<T> list) {
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			//Update the timestamp for last change
			updateTimestamp(newTimestamp);
			
			//Get the data key
			DataKey dataKey = getDataKey();
			
			//Get the data converter
			DefaultListDataConverter<T> converter = this.converter;
			
			//Conversion to redis and mongoDB
			String dataString = converter.convertToDatabase(list);
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
			setData(list);
		} finally {
			if(jedis != null) jedis.close();
		}
	}
	
	/**
	 * Add an element to the {@link List}
	 * 
	 * @param e The element that will be added to {@link List}
	 */
	public void add(T element) {
		short hashslot = getDataKey().getHashslot();
		//TODO
		hashslot = 100;
		
		if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
			setData(new ArrayList<T>(getList()));
		}
		
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		//Updated list (clone)
		@SuppressWarnings("unchecked")
		ArrayList<T> temp = (ArrayList<T>) getData().clone();
		temp.add(element);
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			//Update the timestamp for last change
			updateTimestamp(newTimestamp);
			
			//Get the data key
			DataKey dataKey = getDataKey();
			
			//Get the data converter
			DefaultListDataConverter<T> converter = this.converter;
			
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
	 * Remove an element from the {@link List}
	 * 
	 * @param e The element that will be removed from {@link List}
	 */
	public void remove(T element) {
		short hashslot = getDataKey().getHashslot();
		
		if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
			setData(new ArrayList<T>(getList()));
		}
		
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		//Updated list (clone)
		@SuppressWarnings("unchecked")
		ArrayList<T> temp = (ArrayList<T>) getData().clone();
		boolean change = temp.remove(element);
		
		if(change) {
			try {
				long newTimestamp = System.currentTimeMillis() - 1;
				
				//Update the timestamp for last change
				updateTimestamp(newTimestamp);
				
				//Get the data key
				DataKey dataKey = getDataKey();
				
				//Get the data converter
				DefaultListDataConverter<T> converter = this.converter;
				
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
	 * Clear the {@link List}
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
			setData(new ArrayList<T>());
		} finally {
			if(jedis != null) jedis.close();
		}
	}
	
	/**
	 * Check if {@link List} contains an element
	 * 
	 * @param element The element to be checked
	 * @return true if {@link List} contains element
	 */
	public boolean contains(T element) {
		List<T> list = getList();
		
		if(list.contains(element)) return true;
		return false;
	}
	
	/**
	 * Get the size of the {@link List}
	 * 
	 * @return The size of the {@link List}
	 */
	public int size() {
		List<T> list = getList();
		
		return list.size();
	}

	/**
	 * Set the element on index i to the given element
	 * 
	 * @param i
	 * @param element
	 */
	public void set(int i, T element) {
		short hashslot = getDataKey().getHashslot();
		//TODO
		hashslot = 100;
		
		if(DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
			setData((ArrayList<T>) getList());
		}
		
		Jedis jedis = JedisFactory.getInstance().getJedis();	
		DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
		
		//Updated list (clone)
		@SuppressWarnings("unchecked")
		ArrayList<T> temp = (ArrayList<T>) getData().clone();
		temp.set(i, element);
		
		try {
			long newTimestamp = System.currentTimeMillis() - 1;
			
			//Update the timestamp for last change
			updateTimestamp(newTimestamp);
			
			//Get the data key
			DataKey dataKey = getDataKey();
			
			//Get the data converter
			DefaultListDataConverter<T> converter = this.converter;
			
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
