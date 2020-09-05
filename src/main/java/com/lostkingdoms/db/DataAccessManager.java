package com.lostkingdoms.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lostkingdoms.db.exceptions.MissingOrganizedEntityKeyException;
import com.lostkingdoms.db.exceptions.MissingOrganizedObjectKeyException;
import com.lostkingdoms.db.exceptions.MissingOrganizedObjectTypeException;
import com.lostkingdoms.db.organization.annotations.OrganizedEntity;
import com.lostkingdoms.db.organization.annotations.OrganizedObject;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.miscellaneous.DataKey;
import com.lostkingdoms.db.organization.objects.OrganizedDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedListDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedMapDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedSingleDataObject;

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
	Map<Class<?>, Map<UUID, com.lostkingdoms.db.organization.OrganizedEntity>> managedEntities;
	
	private DataAccessManager() {
		managedEntities = new HashMap<Class<?>, Map<UUID,com.lostkingdoms.db.organization.OrganizedEntity>>();
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
	public boolean isCached(Class<?> clazz, UUID identifier) {
		if(managedEntities.containsKey(clazz)) {
			if(managedEntities.get(clazz).containsKey(identifier)) return true;
		}
		return false;
	}
	
	/**
	 * Get a managed entity by it's class and uuid.
	 * 
	 * @param clazz
	 * @param id
	 */
	public com.lostkingdoms.db.organization.OrganizedEntity getEntity(Class<?> clazz, UUID identifier) {
		//Check if clazz is OrganizedEntity
		if(clazz.getAnnotation(OrganizedEntity.class) == null) {
			//TODO Error
			return null;
		}
		
		//If clazz extends the OrganizedEntity class
		if(clazz.getSuperclass() != com.lostkingdoms.db.organization.OrganizedEntity.class
				|| clazz == com.lostkingdoms.db.organization.OrganizedEntity.class) {
			//TODO Error
			return null;
		}
		
		//Check if this entity already exists an return it if so
		if(managedEntities.containsKey(clazz)) {
			if(managedEntities.get(clazz).containsKey(identifier)) {
				return managedEntities.get(clazz).get(identifier);
			}
		} 
		
		//Object does not exist -> Create it
		com.lostkingdoms.db.organization.OrganizedEntity orgEntity = buildOrganizedEntity(clazz, identifier);
		
		//Save created object to managedEntity Map
		if(managedEntities.containsKey(clazz)) {
			managedEntities.get(clazz).put(identifier, orgEntity);
		} else {
			Map<UUID, com.lostkingdoms.db.organization.OrganizedEntity> map = new HashMap<UUID, com.lostkingdoms.db.organization.OrganizedEntity>();
			map.put(identifier, orgEntity);
			
			managedEntities.put(clazz, map);
		}
	
		return orgEntity;
	}
	
	/**
	 * Remove a managed entity from local cache
	 * 
	 * @param clazz
	 * @param identifier
	 */
	public void removeEntity(Class<?> clazz, UUID identifier) {
		if(managedEntities.containsKey(clazz)) {
			managedEntities.get(clazz).remove(identifier);
		}
	}
	
	/**
	 * Get all locally cached entities by class
	 * 
	 * @param clazz
	 * @return
	 */
	public List<com.lostkingdoms.db.organization.OrganizedEntity> getAllCachedEntities(Class<?> clazz) {
		if(managedEntities.containsKey(clazz)) return (List<com.lostkingdoms.db.organization.OrganizedEntity>) managedEntities.get(clazz).values();
		return new ArrayList<com.lostkingdoms.db.organization.OrganizedEntity>();
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
	private com.lostkingdoms.db.organization.OrganizedEntity buildOrganizedEntity(Class<?> clazz, UUID identifier) {
		// Check if requested class is OrganizedEntity
		if(clazz.getAnnotation(OrganizedEntity.class) == null) return null;
		
		// Build object with reflection
		try {
			Constructor<?> constr = clazz.getConstructor(UUID.class);

			com.lostkingdoms.db.organization.OrganizedEntity obj = (com.lostkingdoms.db.organization.OrganizedEntity) constr.newInstance(identifier);
			
			for(Field field : obj.getClass().getDeclaredFields()) {
				if(field.getAnnotation(OrganizedObject.class) != null) {
					OrganizedEntity entAnn = clazz.getAnnotation(OrganizedEntity.class);
					OrganizedObject objAnn = field.getAnnotation(OrganizedObject.class); 
					
					if(entAnn.key() != null && objAnn.key() != null) {
						DataKey dataKey = new DataKey(entAnn.key(), objAnn.key(), identifier);
						
						if(objAnn.organizationType() == null) throw new MissingOrganizedObjectTypeException(clazz.getSimpleName() + "." + field.getName());
						OrganizationType orgType = objAnn.organizationType();
						
						Constructor<?> fConstr = null;
						if(field.getType() == OrganizedSingleDataObject.class) {
							fConstr = OrganizedSingleDataObject.class.getConstructor(DataKey.class, OrganizationType.class);
						}
						if(field.getType() == OrganizedListDataObject.class) {
							fConstr = OrganizedListDataObject.class.getConstructor(DataKey.class, OrganizationType.class);
						}
						if(field.getType() == OrganizedMapDataObject.class) {
							fConstr = OrganizedMapDataObject.class.getConstructor(DataKey.class, OrganizationType.class);
						}	

						OrganizedDataObject<?> orgObj = (OrganizedDataObject<?>) fConstr.newInstance(dataKey, orgType);
						field.setAccessible(true);
						field.set(obj, orgObj);
						field.setAccessible(false);
					} else {
						if(entAnn.key() == null) {
							throw new MissingOrganizedEntityKeyException(clazz.getSimpleName());
						}
						if(objAnn.key() == null) {
							throw new MissingOrganizedObjectKeyException(clazz.getSimpleName() + "." + field.getName());
						}
					}
					
				} 
			}
			
			return obj;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException  
				| MissingOrganizedEntityKeyException | MissingOrganizedObjectKeyException | MissingOrganizedObjectTypeException e) {
			e.printStackTrace();
		} 
		
		return null;
		
	}
	
}
