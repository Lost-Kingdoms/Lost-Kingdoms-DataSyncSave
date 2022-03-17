package com.lostkingdoms.db;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.converters.impl.OrganizedEntityConverter;
import com.lostkingdoms.db.errors.ConverterAlreadyRegisteredError;
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
	public static String syncMessageChannel;

	/**
	 * The name of the mongodb
	 */
	public static String mongoDBName;

	/**
	 * The number of the redis database
	 */
	public static int redisDBNumber;

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
	private DataOrganizationManager() {
		LKLogger.getInstance().setLevel(LogLevel.INFO);
		LKLogger.getInstance().setLogType(LogType.ALL);

		try {
			Properties properties = new Properties();
			properties.loadFromXML(new FileInputStream("database_config.xml"));
			mongoDBName = properties.getProperty("mongodb_name");
			redisDBNumber = Integer.parseInt(properties.getProperty("redis_database_number"));
			syncMessageChannel = properties.getProperty("sync_message_channel_name");
			LKLogger.getInstance().info("Database config loaded", LogType.STARTUP);
		} catch (Exception e) {
			try {
				Properties properties = new Properties();
				properties.setProperty("mongodb_name", "lostkingdoms");
				properties.setProperty("redis_database_number", "0");
				properties.setProperty("sync_message_channel_name", "LostKingdoms_Sync");
				properties.storeToXML(new FileOutputStream("database_config.xml"), "");
				LKLogger.getInstance().info("Default database config created", LogType.STARTUP);
			} catch (Exception e2) {
				LKLogger.getInstance().error("Config file could not be created", LogType.STARTUP);
			}
		}

		LKLogger.getInstance().info("Lost-Kingdoms-DataSync Starting", LogType.STARTUP);
		LKLogger.getInstance().info("MongoDB starting up", LogType.STARTUP);
		MongoDBFactory.getInstance();
		LKLogger.getInstance().info("MongoDB succesfully started", LogType.STARTUP);
		
		try {
			converters = new HashMap<>();
			instanceID = UUID.randomUUID();
			LKLogger.getInstance().debug("Session id is" + instanceID.toString(), LogType.STARTUP);
			lastUpdated = new long[HASH_SLOT_COUNT];
			
			new Thread(() -> {
				LKLogger.getInstance().info("Jedis starting up", LogType.STARTUP);
				Jedis jedis = JedisFactory.getInstance().getJedis();
				LKLogger.getInstance().info("Jedis succesfully started", LogType.STARTUP);

				LKLogger.getInstance().debug("Sync Listener subscribed", LogType.STARTUP);
				jedis.subscribe(new DataSyncListener(), syncMessageChannel);

				LKLogger.getInstance().warn("Sync Listener closed!", LogType.STARTUP);
				jedis.quit();
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
	
	public void invalidateHashSlot(int slot) {
		lastUpdated[slot] = System.currentTimeMillis();
	}
	
	/**
	 * Get the timestamp when a hashslot was last updated
	 * 
	 * @param hashSlot the hashslot to check
	 * @return Last update timestamp
	 */
	public long getLastUpdated(int hashSlot) {
		return lastUpdated[hashSlot];
	}
	
	/**
	 * Register a {@link OrganizedEntity} and it's corresponding {@link OrganizedEntityConverter}
	 * 
	 * @param clazz the class the {@link OrganizedEntityConverter} supports
	 * @param converter the {@link OrganizedEntityConverter} to register
	 */
	public <T> void registerOrganizedEntity(Class<T> clazz, OrganizedEntityConverter<T> converter) {
		registerDataConverter(clazz, converter);
	}
	
	/**
	 * Registers an {@link AbstractDataConverter}
	 * 
	 * @param converter the {@link {@link AbstractDataConverter} to register
	 */
	public <T> void registerDataConverter(Class<T> clazz, AbstractDataConverter<T> converter) {
		if(hasDataConverter(clazz)) throw new ConverterAlreadyRegisteredError(clazz);
		converters.put(clazz, converter);
		LKLogger.getInstance().debug("Data Converter registered: " + clazz.getSimpleName(), LogType.STARTUP);
	}
	
	/**
	 * Get a converter for a class
	 * 
	 * @param clazz the class to get a converter from
	 * @return The suitable converter or {@link DefaultDataConverter}
	 */
	@SuppressWarnings("unchecked")
	public <T> AbstractDataConverter<T> getDataConverter(Class<T> clazz) {
		return (AbstractDataConverter<T>) converters.getOrDefault(clazz, null);
	}
	
	/**
	 * Check if there exists a converter for a class
	 * 
	 * @param clazz the class to check
	 * @return True, if converter exists
	 */
	public <T> boolean hasDataConverter(Class<T> clazz) {
		return converters.containsKey(clazz);
	}
	
	/**
	 * Get this instances {@link UUID}
	 * 
	 * @return the instances {@link UUID}
	 */
	public UUID getInstanceID() {
		return instanceID;
	}
}
