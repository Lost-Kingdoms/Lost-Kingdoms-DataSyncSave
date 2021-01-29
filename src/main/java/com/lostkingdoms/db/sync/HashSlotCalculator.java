package com.lostkingdoms.db.sync;

import redis.clients.jedis.util.JedisClusterCRC16;

/**
 * Utility class to calculate a hashslot a key is in
 *
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
public final class HashSlotCalculator {
	
	private HashSlotCalculator() {
		throw new IllegalStateException("Utility class");
	}
	
	/**
	 * Return the redis cluster hashslot for a key
	 * 
	 * @param key the key to calculate the hashslot for
	 * @return the hashslot for the key
	 */
	public static int calculateHashSlot(String key) {
		return JedisClusterCRC16.getSlot(key);
    }

}
