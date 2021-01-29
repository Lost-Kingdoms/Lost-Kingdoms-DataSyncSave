package com.lostkingdoms.db.factories;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * Provider class for {@link MongoClient} and it's {@link DB} instances
 *
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
public final class MongoDBFactory {

	/** The singletons instance */
	private static MongoDBFactory instance;
	
	/** The thread save {@link MongoClient} */
	private MongoClient mongoClient;
	
	/** The name of the database */
	private static final String DATABASE_NAME = "lostkingdoms";
	
	
	
	/**
	 * Constructor.
	 * Initiates the mongoDB connection.
	 */
	private MongoDBFactory() {
		try {
			this.mongoClient = new MongoClient();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the {@link MongoClient}
	 * 
	 * @return the mongo client
	 */
	public MongoClient getMongoClient() {
		return this.mongoClient;
	}
	
	/**
	 * Get the mongo database (Creates it if it doesn't exist)
	 * 
	 * @return the database
	 */
	public DB getMongoDatabase() {
		try {
			mongoClient.getAddress();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return getMongoClient().getDB(DATABASE_NAME);
	}
	
	 /**
     * Get the instance of this factory
     * 
     * @return The instance
     */
	public static MongoDBFactory getInstance() {
		if(instance == null) instance = new MongoDBFactory();
		return instance;
	}
}
