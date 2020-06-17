package com.lostkingdoms.db;

import java.util.ArrayList;
import java.util.List;

import com.lostkingdoms.db.converters.IDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultDataConverter;

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
	private static final String SYNC_MESSAGE_CHANNEL = "LostKingdoms_Sync";
	
	/**
	 * Array of timestamps for all hashslots
	 */
	private long[] lastUpdated = new long[HASH_SLOT_COUNT];
	
	/**
	 * The list of all registered converters
	 */
	private List<IDataConverter> converters;
	
	
	
	/**
	 * Constructor of the {@link DataOrganizationManager}
	 */
	public DataOrganizationManager() {
		converters = new ArrayList<IDataConverter>();	
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
	public void registerDataConverter(IDataConverter dataConverter) {
		converters.add(dataConverter);
	}
	
	/**
	 * Get a converter for a class
	 * 
	 * @param clazz
	 * @return the suitable converter or {@link DefaultDataConverter}
	 */
	public IDataConverter getDataConverter(Class<?> clazz) {
		for(IDataConverter dataConverter : converters) {
			//TODO
		}
		
		//No converter for this class was found
		
		return new DefaultDataConverter();
	}
}
