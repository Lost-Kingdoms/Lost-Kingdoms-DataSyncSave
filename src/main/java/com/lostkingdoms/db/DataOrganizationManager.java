package com.lostkingdoms.db;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.converters.impl.OrganizedEntityConverter;
import com.lostkingdoms.db.factories.JedisFactory;
import com.lostkingdoms.db.factories.MongoDBFactory;
import com.lostkingdoms.db.logger.LKLogger;
import com.lostkingdoms.db.logger.LogLevel;
import com.lostkingdoms.db.logger.LogType;
import com.lostkingdoms.db.organization.annotations.OrganizedEntity;
import com.lostkingdoms.db.sync.DataSyncListener;

import redis.clients.jedis.Jedis;

/**
 * The core class of this API. Manages some constants, sets up synchronization on
 * intitialisation. 
 * Manages the registration of {@link OrganizedEntity}s and {@link AbstractDataConverter}s
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
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
		instance = this;
		LKLogger.getInstance().setLevel(LogLevel.DEBUG);
		LKLogger.getInstance().setLogType(LogType.ALL);
		
		LKLogger.getInstance().info("Lost-Kingdoms-DataSync Starting", LogType.STARTUP);
		
		LKLogger.getInstance().info("MongoDB starting up", LogType.STARTUP);
		MongoDBFactory.getInstance();
		LKLogger.getInstance().info("MongoDB succesfully started", LogType.STARTUP);
		
		try {
			converters = new HashMap<Class<?>, AbstractDataConverter<?>>();
			instanceID = UUID.randomUUID();
			LKLogger.getInstance().debug("Session id is" + instanceID.toString(), LogType.STARTUP);
			lastUpdated = new long[HASH_SLOT_COUNT];
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					LKLogger.getInstance().info("Jedis starting up", LogType.STARTUP);
					Jedis jedis = JedisFactory.getInstance().getJedis();
					LKLogger.getInstance().info("Jedis succesfully started", LogType.STARTUP);
					
					LKLogger.getInstance().debug("Sync Listener subscribed", LogType.STARTUP);
					jedis.subscribe(new DataSyncListener(), SYNC_MESSAGE_CHANNEL);
					
					LKLogger.getInstance().warn("Sync Listener closed!", LogType.STARTUP);
					jedis.quit();
				}
			}, "sync_Listener").start();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		LKLogger.getInstance().info("Lost-Kingdoms-DataSync succesfully started", LogType.STARTUP);
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
	 * Register a {@link OrganizedEntity} and it's corresponding {@link OrganizedEntityConverter}
	 * 
	 * @param clazz
	 * @param converter
	 */
	public void registerOrganizedEntity(Class<?> clazz, 
			OrganizedEntityConverter<?> converter) {
		registerDataConverter(clazz, converter);
	}
	
	/**
	 * Registers an {@link IDataConverter} 
	 * 
	 * @param dataConverter
	 */
	public void registerDataConverter(Class<?> clazz, AbstractDataConverter<?> converter) {
		converters.put(clazz, converter);
		LKLogger.getInstance().debug("Data Converter registered: " + clazz.getSimpleName(), LogType.STARTUP);
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
