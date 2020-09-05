package com.lostkingdoms.db.organization;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.converters.impl.OrganizedObjectConverter;
import com.lostkingdoms.db.database.JedisFactory;
import com.lostkingdoms.db.sync.DataSyncListener;

import redis.clients.jedis.Jedis;

/**
 * The core class of this API. Manages some constants, sets up synchronization on
 * intitialisation. 
 * Manages the registration of {@link OrganizedEntity}s and {@link AbstractDataConverter}s
 * 
 * @author Tim
 *
 */
public final class DataOrganizationManager {

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
	private Map<Class<?>, AbstractDataConverter<?>> converters;
	
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
			converters = new HashMap<Class<?>, AbstractDataConverter<?>>();
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
	 * @return Last update timestamp
	 */
	public long getLastUpdated(int hashslot) {
		return lastUpdated[hashslot];
	}
	
	/**
	 * Register a {@link OrganizedEntity} and it's corresponding {@link OrganizedObjectConverter}
	 * 
	 * @param clazz
	 * @param converter
	 */
	public void registerOrganizedEntity(Class<? extends OrganizedEntity> clazz, 
			OrganizedObjectConverter<OrganizedEntity> converter) {
		registerDataConverter(clazz, converter);
	}
	
	/**
	 * Registers an {@link IDataConverter} 
	 * 
	 * @param dataConverter
	 */
	public void registerDataConverter(Class<?> clazz, AbstractDataConverter<?> converter) {
		converters.put(clazz, converter);
	}
	
	/**
	 * Get a converter for a class
	 * 
	 * @param clazz
	 * @return The suitable converter or {@link DefaultDataConverter}
	 */
	public AbstractDataConverter<?> getDataConverter(Class<?> clazz) {
		if(converters.get(clazz) != null) 
			return converters.get(clazz);
		
		return null;
	}
	
	/**
	 * Check if there exists a converter for a class
	 * 
	 * @param clazz
	 * @return True, if converter exists
	 */
	public boolean hasDataConverter(Class<?> clazz) {
		if(converters.containsKey(clazz)) return true;
		return false;
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
