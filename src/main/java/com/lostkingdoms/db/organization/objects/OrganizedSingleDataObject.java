package com.lostkingdoms.db.organization.objects;

import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.factories.JedisFactory;
import com.lostkingdoms.db.factories.MongoDBFactory;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;
import com.mongodb.*;

import redis.clients.jedis.Jedis;

import javax.annotation.Nullable;

/**
 * The base class for single data which should be saved or synced.
 * (no list, map)
 *
 * @param <T> the class of the data that should be synced
 * @author Tim Küchler (https://github.com/TimK1998)
 */
public final class OrganizedSingleDataObject<T> extends OrganizedDataObject<T> {

    /**
     * The {@link DefaultDataConverter} that will be used for serialization and
     * deserialization.
     */
    private final DefaultDataConverter<T> converter;


    /**
     * Constructor for {@link OrganizedSingleDataObject}.
     * This represent a single object that should be organized (no list, map).
     *
     * @param dataKey          The objects {@link DataKey}
     * @param organizationType The objects {@link OrganizationType}
     */
    public OrganizedSingleDataObject(DataKey dataKey, OrganizationType organizationType, DefaultDataConverter<T> converter) {
        setDataKey(dataKey);
        this.converter = converter;
        setOrganizationType(organizationType);

        //new Thread(this::get).start();
    }

    /**
     * The public getter method for the data.
     * Checks consistency of data with global cache and DB.
     *
     * @return the data of this {@link OrganizedSingleDataObject}
     */
    @Nullable
    public T get() {
        if (!doesExist) return null;

        // If data is up-to-date
        int hashslot = getDataKey().getHashslot();
        if ((DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() && getTimestamp() != 0)
                || getOrganizationType() == OrganizationType.NONE) {
            return getData();
        }

        try (Jedis jedis = JedisFactory.getInstance().getJedis()) {
            long newTimestamp = System.currentTimeMillis() - 1;

            // Data is not up-to-date or null
            // Try to get data from redis global cache
            String dataString = jedis.get(getDataKey().getRedisKey());

            // Check if data is null
            if (dataString != null) {
                //Convert the data
                T newData = converter.convertFromDatabase(dataString);

                //Conversion failed
                if (newData == null) {
                    doesExist = false;
                    return null;
                }

                //Set the new data
                setData(newData);

                //Update timestamp to indicate when data was last updated
                updateTimestamp(newTimestamp);

                return getData();
            }

            // Data in global cache is null
            // Try to get data from MongoDB
            if (getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) {
                DataKey dataKey = getDataKey();
                DB mongodb = MongoDBFactory.getInstance().getMongoDatabase();

                DBCollection collection = mongodb.getCollection(dataKey.getMongoDBCollection());
                BasicDBObject query = new BasicDBObject();
                query.put(IDENTIFIER, dataKey.getMongoDBIdentifier());

                DBObject object = collection.findOne(query);

                if (object != null) {
                    dataString = (String) object.get(dataKey.getMongoDBValue());
                }

                //Check if data is null
                if (dataString != null) {
                    //Convert the data
                    T newData = converter.convertFromDatabase(dataString);

                    //Conversion failed
                    if (newData == null) {
                        doesExist = false;
                        return null;
                    }

                    //Set the new data
                    setData(newData);

                    //Update timestamp to indicate when data was last updated
                    updateTimestamp(newTimestamp);

                    //Push data to Redis
                    jedis.set(dataKey.getRedisKey(), dataString);

                    return getData();
                }
            }

            doesExist = false;
            return null;
        }
    }

    /**
     * The public setter for the data. Processes the data according to
     * {@link OrganizationType}.
     *
     * @param data the data to set
     */
    public void set(T data) {
        doesExist = true;
        long newTimestamp = System.currentTimeMillis() - 1;

        //Update the timestamp for last change
        updateTimestamp(newTimestamp);

        // Send to Cache and DB
        new Thread(() -> {
            try (Jedis jedis = JedisFactory.getInstance().getJedis()) {

                //Get the data key
                DataKey dataKey = getDataKey();

                //Conversion to redis and mongoDB
                String dataString = converter.convertToDatabase(data);
                if (dataString == null) {
                    return;
                }

                //Update to redis
                if (getOrganizationType() == OrganizationType.SYNC || getOrganizationType() == OrganizationType.BOTH) {
                    if (dataString.equals("")) {
                        jedis.del(dataKey.getRedisKey());
                    } else {
                        jedis.set(dataKey.getRedisKey(), dataString);
                    }
                }

                //Update to MongoDB
                if (getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) {
                    DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
                    DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());

                    while (true) {
                        try {
                            //Test if object already exists
                            BasicDBObject query = new BasicDBObject();
                            query.put(IDENTIFIER, dataKey.getMongoDBIdentifier());

                            DBObject object = collection.findOne(query);

                            if (object != null) {
                                query = new BasicDBObject();
                                query.put(IDENTIFIER, dataKey.getMongoDBIdentifier());

                                BasicDBObject update;
                                if (dataString.equals("")) {
                                    BasicDBObject newDoc = new BasicDBObject();
                                    newDoc.put(dataKey.getMongoDBValue(), "");

                                    update = new BasicDBObject();
                                    update.put("$unset", newDoc);
                                } else {
                                    BasicDBObject newDoc = new BasicDBObject();
                                    newDoc.put(dataKey.getMongoDBValue(), dataString);

                                    update = new BasicDBObject();
                                    update.put("$set", newDoc);
                                }

                                collection.update(query, update);
                            } else {
                                if (!dataString.equals("")) {
                                    BasicDBObject create = new BasicDBObject();
                                    create.put(IDENTIFIER, dataKey.getMongoDBIdentifier());
                                    create.put(MONGO_IDENTIFIER, dataKey.getMongoDBIdentifier());
                                    create.put(dataKey.getMongoDBValue(), dataString);

                                    collection.insert(create);
                                }
                            }

                            break;
                        } catch (DuplicateKeyException ignored) {
                        }
                    }
                }

                //Publish to other servers via redis
                sendSyncMessage(jedis);
            }
        }).start();

        if (data == null) {
            doesExist = false;
        }

        //Set the local data
        setData(data);
    }

}
