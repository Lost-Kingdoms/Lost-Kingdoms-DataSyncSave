package com.lostkingdoms.db.organization.miscellaneous;

import java.util.UUID;

import com.lostkingdoms.db.organization.objects.OrganizedSingleDataObject;
import com.lostkingdoms.db.sync.HashSlotCalculator;

/**
 * Class that calculates the key of a {@link OrganizedSingleDataObject} for redis
 * and MongoDB.
 * Also contains the redis hashslot of the key and the identifier of the organized
 * Entity or Object.
 */
public final class DataKey {

	/**
	 * The main key of the the data structure e.g. user
	 */
	private String mainKey;
	
	/**
	 * The sub key of the data structure e.g. level
	 */
	private String subKey;
	
	/**
	 * The identifier of the OrganizedEntity
	 */
	private Object identifier;
	
	/**
	 * The hashslot of the redis key
	 */
	private short redisHashslot;
	
	
	
	/**
	 * Construct a new {@link DataKey}
	 * 
	 * @param mainKey
	 * @param subKey
	 * @param identifier
	 */
	public DataKey(String mainKey, String subKey, Object identifier) {
		this.mainKey = mainKey;
		this.subKey = subKey;
		this.identifier = identifier;
		this.redisHashslot = HashSlotCalculator.calculateHashSlot(getRedisKey());
	}
	
	/**
	 * Get the redis representation of this key
	 * 
	 * @return the redis key
	 */
	public String getRedisKey() {
		return (this.mainKey + "." + this.subKey + "." + identifierToString());
	}
	
	/**
	 * Get the mongoDB collection for this key
	 * 
	 * @return the mongoDB collection
	 */
	public String getMongoDBCollection() {
		return this.mainKey.toLowerCase();
	}
	
	/**
	 * Get the mongoDB identifier
	 * 
	 * @return the mongoDB identifier
	 */
	public String getMongoDBIdentifier() {
		return identifierToString();
	}
	
	private String identifierToString() {
		Class<?> idClass = this.identifier.getClass();
		
		if(idClass == UUID.class) return ((UUID)this.identifier).toString();
		if(idClass == String.class) return (String) this.identifier;
		if(idClass.isEnum()) return ((Enum<?>)this.identifier).name();
		return "";
	}
	
	/**
	 * Get the mongoDB value name
	 * 
	 * @return the mongoDB value name
	 */
	public String getMongoDBValue() {
		return this.subKey.toLowerCase();
	}
	
	/**
	 * Get the redis hashslot of this key
	 * 
	 * @return
	 */
	public short getHashslot() {
		return this.redisHashslot;
	}
}
