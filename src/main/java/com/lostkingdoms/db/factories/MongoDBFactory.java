package com.lostkingdoms.db.factories;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * Provider class for {@link MongoClient} and it's {@link DB} instances
 */
public final class MongoDBFactory {

	/**
	 * The singletons instance
	 */
	private static MongoDBFactory instance;
	
	/**
	 * The thread save {@link MongoClient}
	 */
	private MongoClient mongoClient;
	
	/**
	 * The name of the database
	 */
	private final String DATABASE_NAME;
	
	
	
	/**
	 * Constructor.
	 * Initiates the mongoDB connection.
	 */
	public MongoDBFactory() {
		try {
			this.mongoClient = new MongoClient();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		//TODO Read db name
		DATABASE_NAME = "Lostkingdoms";
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
