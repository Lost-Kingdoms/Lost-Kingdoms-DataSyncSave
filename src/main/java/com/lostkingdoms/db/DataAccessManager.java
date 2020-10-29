package com.lostkingdoms.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultListDataConverter;
import com.lostkingdoms.db.converters.impl.DefaultMapDataConverter;
import com.lostkingdoms.db.exceptions.NoOrganizedEntityException;
import com.lostkingdoms.db.exceptions.WrongIdentifierException;
import com.lostkingdoms.db.exceptions.WrongMethodUseException;
import com.lostkingdoms.db.factories.JedisFactory;
import com.lostkingdoms.db.factories.MongoDBFactory;
import com.lostkingdoms.db.organization.annotations.OrganizedEntity;
import com.lostkingdoms.db.organization.annotations.OrganizedObject;
import com.lostkingdoms.db.organization.annotations.OrganizedSuperentity;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;
import com.lostkingdoms.db.organization.miscellaneous.OrganizedEntityInformation;
import com.lostkingdoms.db.organization.miscellaneous.OrganizedObjectInformation;
import com.lostkingdoms.db.organization.objects.OrganizedDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedListDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedMapDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedSingleDataObject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import redis.clients.jedis.Jedis;

/**
 * Manager that handles the creation, destruction and query of all organized data types and entities
 * 
 * @author Tim
 *
 */
public final class DataAccessManager {
	
	/**
	 * The singletons instance
	 */
	private static DataAccessManager instance;
	
	/**
	 * A map containing all created entities mapped by it's class
	 */
	private Map<Class<?>, Map<Object, Object>> managedEntities;
	
	
	
	private DataAccessManager() {
		instance = this;
		managedEntities = new HashMap<Class<?>, Map<Object, Object>>();
	}
	
	
	
	/**
	 * Get the instance of this manager
	 * 
	 * @return The instance
	 */
	public static DataAccessManager getInstance() {
		if(instance == null) instance = new DataAccessManager();
		return instance;
	}
	
	
	
	/**
	 * Check if a entity is locally cached
	 * 
	 * @param clazz
	 * @param identifier
	 * @return true if the entity is locally cached
	 */
	public boolean isCached(Class<?> clazz, Object identifier) {
		if(managedEntities.containsKey(clazz)) {
			if(managedEntities.get(clazz).containsKey(identifier)) return true;
		}
		return false;
	}
	
	
	
	/**
	 * Checks if an clazz with given identifier exists in database
	 * 
	 * @param clazz
	 * @param identifier
	 * @return
	 */
	public boolean isInDatabase(Class<?> clazz, Object identifier) {
		try {
			OrganizedEntityInformation info = new OrganizedEntityInformation(clazz);
			
			DB db = MongoDBFactory.getInstance().getMongoDatabase();
			DBCollection collection = db.getCollection(info.getEntityKey());
			
			BasicDBObject obj = new BasicDBObject();
			obj.put("identifier", info.identifierToString(identifier));
			if(collection.find(obj).count() == 0) return false;
			return true;
		} catch (NoOrganizedEntityException | WrongIdentifierException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	

	/**
	 * Remove class with identifier from local cahce, redis and database
	 * 
	 * @param clazz
	 * @param identifier
	 */
	public void removeEntity(Class<?> clazz, Object identifier) {
		Jedis jedis = JedisFactory.getInstance().getJedis();
		try {
			OrganizedEntityInformation info = new OrganizedEntityInformation(clazz);
			
			//Local cache
			if(managedEntities.containsKey(clazz)) {
				managedEntities.get(clazz).remove(identifier);
			}
			
			//Redis		
			for(OrganizedObjectInformation i : info.getOrganizedObjectFields()) {
				jedis.del(info.getEntityKey() + "." + i.getObjectKey() + "." + info.identifierToString(identifier));
			}
			
			//MongoDB
			DBCollection collection = MongoDBFactory.getInstance().getMongoDatabase().getCollection(info.getEntityKey());
			BasicDBObject query = new BasicDBObject();
			query.put("identifier", info.identifierToString(identifier));
			collection.remove(query);
		} catch (WrongIdentifierException | NoOrganizedEntityException e) {
			e.printStackTrace();
		} finally {
			jedis.close();
		}
	}
	
	
	
	/**
	 * Get all entities of class T from the database
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getAllEntities(Class<T> clazz) {
		try {
			OrganizedEntityInformation info = new OrganizedEntityInformation(clazz);
			
			DB mongodb = MongoDBFactory.getInstance().getMongoDatabase();
			DBCursor cur = mongodb.getCollection(info.getEntityKey()).find();
			
			List<T> entityList = new ArrayList<T>();
			for(DBObject obj : cur) {
				entityList.add((T) getEntity(clazz, info.stringToIdentifier((String) obj.get("identifier"))));
			}
			
			return entityList;
		} catch (NoOrganizedEntityException e) {
			e.printStackTrace();
		}
		
		return new ArrayList<T>();
	}
	
	
	
	/**
	 * Gets all locally cached entities by class
	 * 
	 * @param clazz
	 * @return
	 */
	public List<Object> getAllCachedEntities(Class<?> clazz) {
		if(managedEntities.containsKey(clazz)) return new ArrayList<Object>(managedEntities.get(clazz).values());
		return new ArrayList<Object>();
	}
	
	
	
	/**
	 * Gets a organized entity by it's class and identifier.
	 * DOES NOT TEST if object with this id exists. Creates a new
	 * one if so.
	 * 
	 * @param <T>
	 * @param clazz
	 * @param id
	 * @return found class or a newly created
	 * 
	 */
	@SuppressWarnings("unchecked")
	public <T> T getEntity(Class<?> clazz, Object identifier) {
		try {
			//Test if clazz is a correct OrganizedEntity
			OrganizedEntityInformation info = new OrganizedEntityInformation(clazz);
			info.getIdentifierClass();
			info.identifierToString(identifier);	
			
			//Check if this entity already exists in local cache an return it if so
			if(managedEntities.containsKey(clazz)) {
				if(managedEntities.get(clazz).containsKey(identifier)) {
					return (T) managedEntities.get(clazz).get(identifier);
				}
			} 
			
			//Object does not exist -> Create it
			Object orgEntity = createEntity(clazz, identifier);
			
			//Save created object to local cache
			if(managedEntities.containsKey(clazz)) {
				managedEntities.get(clazz).put(identifier, orgEntity);
			} else {
				Map<Object, Object> map = new HashMap<Object, Object>();
				map.put(identifier, orgEntity);
				managedEntities.put(clazz, map);
			}
		
			return (T) orgEntity;
		} catch (NoOrganizedEntityException | WrongIdentifierException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	
	/**
	 * Gets a organized entity from local cache by it's class and identifier
	 * 
	 * @param <T>
	 * @param clazz
	 * @param identfier
	 * @return the found object or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T getCachedEntity(Class<T> clazz, Object identfier) {
		if(managedEntities.containsKey(clazz)) return (T) managedEntities.get(clazz).get(identfier);
		return null;
	}

	
	
	/**
	 * Builds an OrgnizedEntity object for the given identifier.
	 * DOES NOT CHECK the existence of the identifier in global cache or db
	 * 
	 * @param clazz
	 * @param identifier
	 * @param organizedDataType
	 * @return builded object or null if clazz is no {@link com.lostkingdoms.db.organization.OrganizedEntity}
	 */
	private Object createEntity(Class<?> clazz, Object identifier) {		
		// Build object with reflection
		try {
			OrganizedEntityInformation info = new OrganizedEntityInformation(clazz);
			
			Constructor<?> constr = clazz.getConstructor();
			Object obj = constr.newInstance();	
			for(OrganizedObjectInformation i : info.getOrganizedObjectFields()) {
				initializeField(info, i, obj, identifier, false);
			}
			
			Field id = info.getIdentifierField();
			id.setAccessible(true);
			id.set(obj, identifier);
			id.setAccessible(false);
			
			return obj;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException | NoOrganizedEntityException 
				| WrongMethodUseException e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
	
	
	/**
	 * Util method you HAVE TO put at first place in every more then one argument constructor
	 * of a {@link OrganizedEntity} or {@link OrganizedSuperentity}
	 * 
	 * @param obj
	 * @param identifier
	 */
	public void initializeEntityFields(Object obj, Object identifier) {
		try {
			OrganizedEntityInformation info = new OrganizedEntityInformation(obj.getClass());
			
			for(OrganizedObjectInformation i : info.getOrganizedObjectFields()) {
				initializeField(info, i, obj, identifier, false);
			}
			
			Field id = info.getIdentifierField();
			id.setAccessible(true);
			id.set(obj, identifier);
			id.setAccessible(false);
		} catch (NoOrganizedEntityException | NoSuchMethodException | SecurityException | IllegalArgumentException 
				| IllegalAccessException | InstantiationException | InvocationTargetException | WrongMethodUseException e) {
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * Util method to intialize fields of {@link OrganizedEntity}. 
	 * 
	 * @param obj
	 * @param identifier
	 * @param none true, if you want to not save this object in any way
	 */
	public void initializeEntityFields(Object obj, Object identifier, boolean none) {
		try {
			OrganizedEntityInformation info = new OrganizedEntityInformation(obj.getClass());
			
			for(OrganizedObjectInformation i : info.getOrganizedObjectFields()) {
				initializeField(info, i, obj, identifier, none);
			}
			
			Field id = info.getIdentifierField();
			id.setAccessible(true);
			id.set(obj, identifier);
			id.setAccessible(false);
		} catch (NoOrganizedEntityException | NoSuchMethodException | SecurityException | IllegalArgumentException 
				| IllegalAccessException | InstantiationException | InvocationTargetException | WrongMethodUseException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Private util method to initialize a {@link OrganizedObject} field
	 * 
	 * @param eInfo
	 * @param oInfo
	 * @param obj
	 * @param identifier
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws InvocationTargetException
	 * @throws WrongMethodUseException
	 */
	private void initializeField(OrganizedEntityInformation eInfo, OrganizedObjectInformation oInfo, Object obj, Object identifier, boolean none) 
			throws NoSuchMethodException, SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, WrongMethodUseException {
		Field f = oInfo.getField();
		
		DataKey dataKey = new DataKey(eInfo.getEntityKey(), oInfo.getObjectKey(), identifier);
		OrganizationType orgType = oInfo.getOrganizationType();
		if(none) orgType = OrganizationType.NONE;
		
		Constructor<?> fConstr = null;
		OrganizedDataObject<?> orgObj = null;
		if(oInfo.getDataObjectClass() == OrganizedSingleDataObject.class) {
			fConstr = OrganizedSingleDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultDataConverter.class);
			DefaultDataConverter<?> conv = new DefaultDataConverter<>(oInfo.getSingleClass());
			
			orgObj = (OrganizedDataObject<?>) fConstr.newInstance(dataKey, orgType, conv);
		}
		else if(oInfo.getDataObjectClass() == OrganizedListDataObject.class) {
			fConstr = OrganizedListDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultListDataConverter.class);
			DefaultListDataConverter<?> conv = new DefaultListDataConverter<>(oInfo.getListClass());

			orgObj = (OrganizedDataObject<?>) fConstr.newInstance(dataKey, orgType, conv);
		}
		else if(oInfo.getDataObjectClass() == OrganizedMapDataObject.class) { 
			fConstr = OrganizedMapDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultMapDataConverter.class);
			DefaultMapDataConverter<?, ?> conv = new DefaultMapDataConverter<>(oInfo.getMapClass().getLeft(), oInfo.getMapClass().getRight());
			
			orgObj = (OrganizedDataObject<?>) fConstr.newInstance(dataKey, orgType, conv);
		}
		
		f.setAccessible(true);
		f.set(obj, orgObj);
		f.setAccessible(false);
	}
	
	
}
