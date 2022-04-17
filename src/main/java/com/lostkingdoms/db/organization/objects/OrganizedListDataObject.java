package com.lostkingdoms.db.organization.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.converters.impl.DefaultListDataConverter;
import com.lostkingdoms.db.factories.JedisFactory;
import com.lostkingdoms.db.factories.MongoDBFactory;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;
import com.mongodb.*;

import redis.clients.jedis.Jedis;

/**
 * An {@link OrganizedDataObject} which represents an {@link ArrayList} of type T.
 * Provides basic list methods. Later it is planned to fully implement {@link List} interface
 *
 * @param <T> the list class
 * @author Tim K�chler (https://github.com/TimK1998)
 */
public final class OrganizedListDataObject<T> extends OrganizedDataObject<ArrayList<T>> {

    /**
     * The {@link DefaultListDataConverter} that will be used for serialization and
     * deserialization.
     */
    private final DefaultListDataConverter<T> converter;


    /**
     * Constructor for {@link OrganizedListDataObject}.
     * This represent a list of objects that should be organized.
     *
     * @param dataKey          The objects {@link DataKey}
     * @param organizationType The objects {@link OrganizationType}
     */
    public OrganizedListDataObject(DataKey dataKey, OrganizationType organizationType, DefaultListDataConverter<T> converter) {
        setDataKey(dataKey);
        this.converter = converter;
        setOrganizationType(organizationType);
        setData(new ArrayList<>());

        //new Thread(this::getList).start();
    }


    /**
     * Get the {@link List}
     *
     * @return An unmodifiable instance of the {@link List}
     */
    public List<T> getList() {

        if (!doesExist) return new ArrayList<>();

        // If data is up-to-date
        int hashslot = getDataKey().getHashslot();
        if ((DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() && getTimestamp() != 0)
                || getOrganizationType() == OrganizationType.NONE) {
            return Collections.unmodifiableList(getData());
        }


        try (Jedis jedis = JedisFactory.getInstance().getJedis()) {
            long newTimestamp = System.currentTimeMillis() - 1;

            // Data is not up-to-date or null
            // Try to get data from redis global cache
            String dataString = jedis.get(getDataKey().getRedisKey());

            // Check if data is null
            if (dataString != null) {
                //Convert the data
                ArrayList<T> newData = (ArrayList<T>) converter.convertFromDatabase(dataString);

                //Conversion failed
                if (newData == null) {
                    return Collections.unmodifiableList(new ArrayList<>());
                }

                //Set the new data
                setData(newData);

                //Update timestamp to indicate when data was last updated
                updateTimestamp(newTimestamp);

                return Collections.unmodifiableList(getData());
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
                    ArrayList<T> newData = (ArrayList<T>) converter.convertFromDatabase(dataString);

                    //Conversion failed
                    if (newData == null) {
                        return Collections.unmodifiableList(new ArrayList<>());
                    }

                    //Set the new data
                    setData(newData);

                    //Update timestamp to indicate when data was last updated
                    updateTimestamp(newTimestamp);

                    //Push data to Redis
                    jedis.set(dataKey.getRedisKey(), converter.convertToDatabase(getData()));

                    return Collections.unmodifiableList(getData());
                }
            }

            //Data does not exist yet
            doesExist = false;
            return new ArrayList<>();
        }
    }

    /**
     * Set an {@link ArrayList}
     *
     * @param list the list to set
     */
    public void setList(ArrayList<T> list) {
        if (list.isEmpty()) {
            clear();
            return;
        }

        doesExist = true;

        long newTimestamp = System.currentTimeMillis() - 1;

        //Update the timestamp for last change
        updateTimestamp(newTimestamp);

        new Thread(() -> {
            try (Jedis jedis = JedisFactory.getInstance().getJedis()) {

                //Get the data key
                DataKey dataKey = getDataKey();

                //Conversion to redis and mongoDB
                String dataString = converter.convertToDatabase(list);
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
        setData(list);
    }

    /**
     * Add an element to the {@link List}
     *
     * @param element The element that will be added to {@link List}
     */
    public void add(T element) {
        doesExist = true;
        int hashslot = getDataKey().getHashslot();

        if (DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
            setData(new ArrayList<>(getList()));
        }

        //Updated list (clone)
        @SuppressWarnings("unchecked")
        ArrayList<T> temp = (ArrayList<T>) getData().clone();
        temp.add(element);

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
     * Remove an element from the {@link List}
     *
     * @param element The element that will be removed from {@link List}
     */
    public void remove(T element) {
        doesExist = true;
        int hashslot = getDataKey().getHashslot();

        if (DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
            setData(new ArrayList<>(getList()));
        }

        //Updated list (clone)
        @SuppressWarnings("unchecked")
        ArrayList<T> temp = (ArrayList<T>) getData().clone();
        boolean change = temp.remove(element);

        if (change) {
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
     * Clear the {@link List}
     */
    public void clear() {
        doesExist = false;
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
                            query.put(IDENTIFIER, dataKey.getMongoDBIdentifier());

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
        setData(new ArrayList<T>());
    }

    /**
     * Check if {@link List} contains an element
     *
     * @param element The element to be checked
     * @return true if {@link List} contains element
     */
    public boolean contains(T element) {
        List<T> list = getList();
        return list.contains(element);
    }

    /**
     * Get the size of the {@link List}
     *
     * @return The size of the {@link List}
     */
    public int size() {
        List<T> list = getList();
        return list.size();
    }

    /**
     * Set the element on index i to the given element
     *
     * @param i       index of the element to replace
     * @param element element to be stored at the specified position
     */
    public void set(int i, T element) {
        doesExist = true;
        int hashslot = getDataKey().getHashslot();

        if (DataOrganizationManager.getInstance().getLastUpdated(hashslot) < getTimestamp() || getTimestamp() == 0) {
            setData(new ArrayList<>(getList()));
        }

        //Updated list (clone)
        @SuppressWarnings("unchecked")
        ArrayList<T> temp = (ArrayList<T>) getData().clone();
        temp.set(i, element);

        if (temp.isEmpty()) {
            clear();
            return;
        }

        new Thread(() -> {
            try (Jedis jedis = JedisFactory.getInstance().getJedis()) {
                long newTimestamp = System.currentTimeMillis() - 1;

                //Update the timestamp for last change
                updateTimestamp(newTimestamp);

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
