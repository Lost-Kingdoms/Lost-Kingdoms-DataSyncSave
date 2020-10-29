package com.lostkingdoms.db.factories;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Provider class for {@link Jedis Pool} and it's {@link Jedis} instances
 */
public final class JedisFactory {

	/**
	 * The singletons instance
	 */
	private static JedisFactory instance;
	
	/**
	 * The thread safe {@link JedisPool}
	 */
	private static JedisPool jedisPool;
	
	
	
	/**
	 * Constructor.
	 * Initiates the jedis connection and pool.
	 */
    public JedisFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        
        jedisPool = new JedisPool(
            "127.0.0.1",
            6379
        );
    }

    /**
     * Get the {@link Jedis Pool}
     * 
     * @return The {@link Jedis Pool}
     */
    public JedisPool getJedisPool() {
        return jedisPool;
    }
    
    /**
     * Get a {@link Jedis} instance
     * 
     * @return A {@link Jedis} instance
     */
    public Jedis getJedis() {
    	try {
    		return jedisPool.getResource();
    	} catch (JedisConnectionException e) {
    		return null;
    	}
    }

    /**
     * Get the instance of this factory
     * 
     * @return The instance
     */
    public static JedisFactory getInstance() {
        if (instance == null) instance = new JedisFactory();        
        return instance;
    }
	
}
