package com.lostkingdoms.db.organization;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.lostkingdoms.db.converters.IDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.database.JedisFactory;
import com.lostkingdoms.db.sync.DataSyncListener;

import redis.clients.jedis.Jedis;

public class DataOrganizationManager {

	/**
	 * The singleton instance
	 */
	private static DataOrganizationManager instance;
	
	/**
	 * number of redis hashslots
	 */
	private static final int HASH_SLOT_COUNT = 16384;

	/**
	 * Name of the redis syncing channel
	 */
	public static final String SYNC_MESSAGE_CHANNEL = "LostKingdoms_Sync";
	
	/**
	 * Array of timestamps for all hashslots
	 */
	private long[] lastUpdated;
	
	/**
	 * The list of all registered converters
	 */
	private Map<Class<?>, IDataConverter<?>> converters;
	
	/**
	 * The UUID that identifies this cache instance
	 */
	private UUID instanceID;
	
	/**
	 * Constructor of the {@link DataOrganizationManager}
	 */
	public DataOrganizationManager() {
		Jedis jedis = JedisFactory.getInstance().getJedis();
		
		try {
			converters = new HashMap<Class<?>, IDataConverter<?>>();
			instanceID = UUID.randomUUID();
			lastUpdated = new long[HASH_SLOT_COUNT];
			
			jedis.subscribe(new DataSyncListener(), SYNC_MESSAGE_CHANNEL);
		} finally {
			jedis.close();
		}
		
	}
	
	/**
	 * Get the singletons instance
	 * 
	 * @return instance
	 */
	public static DataOrganizationManager getInstance() {
		if(instance == null) instance = new DataOrganizationManager();
		return instance;
	}
	
	public void invalidateHashSlot(short slot) {
		lastUpdated[slot] = System.currentTimeMillis();
	}
	
	/**
	 * Get the timestamp when a hashslot was last updated
	 * 
	 * @param hashslot
	 * @return last update timestamp
	 */
	public long getLastUpdated(int hashslot) {
		return lastUpdated[hashslot];
	}
	
	/**
	 * Registers an {@link IDataConverter} 
	 * 
	 * @param dataConverter
	 */
	public void registerDataConverter(Class<?> clazz, IDataConverter<?> dataConverter) {
		converters.put(clazz, dataConverter);
	}
	
	/**
	 * Get a converter for a class
	 * 
	 * @param clazz
	 * @return the suitable converter or {@link DefaultDataConverter}
	 */
	public IDataConverter<?> getDataConverter(Class<?> clazz) {
		if(converters.get(clazz) != null) 
			return converters.get(clazz);
		
		//No converter for this class was found	
		return new DefaultDataConverter();
	}
	
	/**
	 * Get this instances ID
	 * 
	 * @return
	 */
	public UUID getInstanceID() {
		return this.instanceID;
	}
}
