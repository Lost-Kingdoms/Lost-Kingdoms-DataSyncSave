package com.lostkingdoms.db.factories;

import com.lostkingdoms.db.DataOrganizationManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Provider class for {@link Jedis Pool} and it's {@link Jedis} instances
 *
 * @author Tim Küchler (https://github.com/TimK1998)
 *
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
    private JedisFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxWaitMillis(50);

        jedisPool = new JedisPool(
                poolConfig,
                "127.0.0.1",
                6379,
                50
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
    	Jedis jedis =  jedisPool.getResource();
        jedis.select(DataOrganizationManager.redisDBNumber);
        return jedis;
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
