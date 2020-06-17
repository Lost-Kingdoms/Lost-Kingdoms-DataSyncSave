package com.lostkingdoms.db.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Provider class for {@link Jedis Pool} and it's {@link Jedis} instances
 */
public class JedisFactory {

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
            poolConfig,
            "localhost"
        );
    }

    /**
     * Get the {@link Jedis Pool}
     * 
     * @return the {@link Jedis Pool}
     */
    public JedisPool getJedisPool() {
        return jedisPool;
    }
    
    /**
     * Get a {@link Jedis} instance
     * 
     * @return a {@link Jedis} instance
     */
    public Jedis getJedis() {
    	return jedisPool.getResource();
    }

    /**
     * Get the instance of this factory
     * 
     * @return the instance
     */
    public static JedisFactory getInstance() {
        if (instance == null) instance = new JedisFactory();        
        return instance;
    }
	
}
