package com.lostkingdoms.db.organization.objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.*;
import org.bson.types.ObjectId;

import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.converters.impl.DefaultMapDataConverter;
import com.lostkingdoms.db.factories.JedisFactory;
import com.lostkingdoms.db.factories.MongoDBFactory;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;

import redis.clients.jedis.Jedis;

/**
 * The base class for data which should be saved or synced as a map.
 * Provides basic map functions. Later it is planned to fully implement {@link Map} interface.
 *
 * @param <K> the key class
 * @param <V> the value class
 * @author Tim Küchler (https://github.com/TimK1998)
 */
public final class OrganizedMapDataObject<K, V> extends OrganizedDataObject<HashMap<K, V>> {

    /**
     * The {@link DefaultMapDataConverter} that will be used for serialization and
     * deserialization.
     */
    private final DefaultMapDataConverter<K, V> converter;


    /**
     * Constructor for {@link OrganizedMapDataObject}.
     * This represent a map of objects that should be organized.
     *
     * @param dataKey          The objects {@link DataKey}
     * @param organizationType The objects {@link OrganizationType}
     */
    public OrganizedMapDataObject(DataKey dataKey, OrganizationType organizationType, DefaultMapDataConverter<K, V> converter) {
        setDataKey(dataKey);
        this.converter = converter;
        setOrganizationType(organizationType);
        setData(new HashMap<>());
        if (!(dataKey.getRedisKey().contains("point") || dataKey.getRedisKey().contains("polygon"))) {
            new Thread(this::getMap).start();
        }
    }

    /**
     * Get the {@link Map}
     *
     * @return An unmodifiable instance of the {@link Map}
     */
    public Map<K, V> getMap() {
        // If data is up-to-date
        if (DOES_FUCKING_NOT_EXIST) {
            return new HashMap<>();
        }
        int hashslot = getDataKey().getHashslot();
        if ((DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() && getTimestamp() != 0)
                || getOrganizationType() == OrganizationType.NONE) {
            return Collections.unmodifiableMap(getData());
        }

        try (Jedis jedis = JedisFactory.getInstance().getJedis()) {
            long newTimestamp = System.currentTimeMillis() - 1;

            // Data is not up-to-date or null
            // Try to get data from redis global cache
            String dataString = jedis.get(getDataKey().getRedisKey());

            // Check if data is null
            if (dataString != null) {
                //Convert the data
                HashMap<K, V> newData = converter.convertFromDatabase(dataString);

                //Conversion failed
                if (newData == null) {
                    return Collections.unmodifiableMap(new HashMap<>());
                }

                //Set the new data
                setData(newData);

                //Update timestamp to indicate when data was last updated
                updateTimestamp(newTimestamp);

                return Collections.unmodifiableMap(getData());
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
                    HashMap<K, V> newData = converter.convertFromDatabase(dataString);

                    //Conversion failed
                    if (newData == null) {
                        return Collections.unmodifiableMap(new HashMap<>());
                    }

                    //Set the new data
                    setData(newData);

                    //Update timestamp to indicate when data was last updated
                    updateTimestamp(newTimestamp);

                    //Push data to Redis
                    jedis.set(dataKey.getRedisKey(), converter.convertToDatabase(getData()));

                    return Collections.unmodifiableMap(getData());
                }
            }

            //Data does not exist yet
            if (getDataKey().getRedisKey().contains("polygon") || getDataKey().getRedisKey().contains("point")) {
                System.out.println("TEEEEST " + getDataKey().getRedisKey());
                DOES_FUCKING_NOT_EXIST = true;
            }
            return getData();
        }
    }

    public void setMap(HashMap<K, V> map) {
        if (map.isEmpty()) {
            clear();
            return;
        }

        long newTimestamp = System.currentTimeMillis() - 1;

        //Update the timestamp for last change
        updateTimestamp(newTimestamp);

        new Thread(() -> {
            try (Jedis jedis = JedisFactory.getInstance().getJedis()) {
                //Get the data key
                DataKey dataKey = getDataKey();

                //Conversion to redis and mongoDB
                String dataString = converter.convertToDatabase(map);
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

                                BasicDBObject newDoc = new BasicDBObject();
                                newDoc.put(dataKey.getMongoDBValue(), dataString);

                                BasicDBObject update = new BasicDBObject();
                                update.put("$set", newDoc);

                                collection.update(query, update);
                            } else {
                                BasicDBObject create = new BasicDBObject();
                                create.put(IDENTIFIER, dataKey.getMongoDBIdentifier());
                                create.put(MONGO_IDENTIFIER, dataKey.getMongoDBIdentifier());
                                create.put(dataKey.getMongoDBValue(), dataString);

                                collection.insert(create);
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

        //Set the local data
        setData(map);
    }

    /**
     * Put a key-value pair to the {@link Map}
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    public void put(K key, V value) {
        int hashslot = getDataKey().getHashslot();

        if (DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
            setData(new HashMap<K, V>(getMap()));
        }

        //Updated list (clone)
        @SuppressWarnings("unchecked")
        HashMap<K, V> temp = (HashMap<K, V>) getData().clone();
        temp.remove(key);
        temp.put(key, value);

        long newTimestamp = System.currentTimeMillis() - 1;

        //Update the timestamp for last change
        updateTimestamp(newTimestamp);

        new Thread(() -> {
            try (Jedis jedis = JedisFactory.getInstance().getJedis()) {
                //Get the data key
                DataKey dataKey = getDataKey();

                //Conversion to redis and mongoDB
                String dataString = converter.convertToDatabase(temp);
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

                                BasicDBObject newDoc = new BasicDBObject();
                                newDoc.put(dataKey.getMongoDBValue(), dataString);

                                BasicDBObject update = new BasicDBObject();
                                update.put("$set", newDoc);

                                collection.update(query, update);
                            } else {
                                BasicDBObject create = new BasicDBObject();
                                create.put(IDENTIFIER, dataKey.getMongoDBIdentifier());
                                create.put(MONGO_IDENTIFIER, dataKey.getMongoDBIdentifier());
                                create.put(dataKey.getMongoDBValue(), dataString);

                                collection.insert(create);
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

        //Set the local data
        setData(temp);
    }

    /**
     * Remove the key from the {@link Map}
     *
     * @param key key whose mapping is to be removed from the map
     */
    public void remove(K key) {
        int hashslot = getDataKey().getHashslot();

        if (DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
            setData(new HashMap<>(getMap()));
        }

        //Updated list (clone)
        @SuppressWarnings("unchecked")
        HashMap<K, V> temp = (HashMap<K, V>) getData().clone();
        Object change = temp.remove(key);

        if (change != null) {
            if (temp.isEmpty()) {
                clear();
                return;
            }

            long newTimestamp = System.currentTimeMillis() - 1;

            //Update the timestamp for last change
            updateTimestamp(newTimestamp);

            new Thread(() -> {
                try (Jedis jedis = JedisFactory.getInstance().getJedis()) {
                    //Get the data key
                    DataKey dataKey = getDataKey();

                    //Conversion to redis and mongoDB
                    String dataString = converter.convertToDatabase(temp);
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

                                    BasicDBObject newDoc = new BasicDBObject();
                                    newDoc.put(dataKey.getMongoDBValue(), dataString);

                                    BasicDBObject update = new BasicDBObject();
                                    update.put("$set", newDoc);

                                    collection.update(query, update);
                                } else {
                                    BasicDBObject create = new BasicDBObject();
                                    create.put(IDENTIFIER, dataKey.getMongoDBIdentifier());
                                    create.put(MONGO_IDENTIFIER, dataKey.getMongoDBIdentifier());
                                    create.put(dataKey.getMongoDBValue(), dataString);

                                    collection.insert(create);
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

            //Set the local data
            setData(temp);
        }
    }

    /**
     * Clear the {@link Map}
     */
    public void clear() {
        long newTimestamp = System.currentTimeMillis() - 1;

        //Update the timestamp for last change
        updateTimestamp(newTimestamp);

        new Thread(() -> {
            try (Jedis jedis = JedisFactory.getInstance().getJedis()) {
                //Get the data key
                DataKey dataKey = getDataKey();

                //Delete from Redis
                jedis.del(dataKey.getRedisKey());

                //Delete from MongoDB
                if (getOrganizationType() == OrganizationType.SAVE_TO_DB || getOrganizationType() == OrganizationType.BOTH) {
                    DB mongoDB = MongoDBFactory.getInstance().getMongoDatabase();
                    DBCollection collection = mongoDB.getCollection(dataKey.getMongoDBCollection());

                    while (true) {
                        try {
                            BasicDBObject query = new BasicDBObject();
                            query.put(IDENTIFIER, new ObjectId(dataKey.getMongoDBIdentifier()));

                            BasicDBObject newDoc = new BasicDBObject();
                            newDoc.put(dataKey.getMongoDBValue(), "");

                            BasicDBObject update = new BasicDBObject();
                            update.put("$set", newDoc);

                            collection.update(query, update);

                            break;
                        } catch (DuplicateKeyException ignored) {
                        }
                    }
                }

                //Publish to other servers via redis
                sendSyncMessage(jedis);
            }
        }).start();

        //Set the local data
        setData(new HashMap<>());
    }

    /**
     * Get a value by key from {@link Map}
     *
     * @param key the key whose associated value is to be returned
     * @return the value V or null if key does not exist
     */
    public V get(K key) {
        Map<K, V> map = getMap();
        return map.get(key);
    }

    /**
     * Check if {@link Map} contains a key
     *
     * @param key key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     */
    public boolean containsKey(K key) {
        Map<K, V> map = getMap();
        return map.containsKey(key);
    }

    /**
     * Check if {@link Map} contains a value
     *
     * @param value value whose presence in this map is to be tested
     * @return true if this map maps one or more keys to the specified value
     */
    public boolean containsValue(V value) {
        Map<K, V> map = getMap();
        return map.containsValue(value);
    }

    /**
     * Get the size of the {@link Map}
     *
     * @return The size of the {@link Map}
     */
    public int size() {
        Map<K, V> map = getMap();
        return map.size();
    }

}
