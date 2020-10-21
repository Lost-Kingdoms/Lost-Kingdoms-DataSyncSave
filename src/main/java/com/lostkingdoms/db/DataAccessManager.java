package com.lostkingdoms.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.logger.LKLogger;
import com.lostkingdoms.db.logger.LogType;
import com.lostkingdoms.db.organization.annotations.Identifier;
import com.lostkingdoms.db.organization.annotations.OrganizedEntity;
import com.lostkingdoms.db.organization.annotations.OrganizedObject;
import com.lostkingdoms.db.organization.annotations.OrganizedSuperentity;
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
	Map<Class<?>, Map<Object, Object>> managedEntities;
	
	private DataAccessManager() {
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
	public Object getEntity(Class<?> clazz, Object identifier) {
		//Check if clazz is OrganizedEntity
		if(clazz.getAnnotation(OrganizedEntity.class) == null) {
			LKLogger.getInstance().warn("Class has no OrgananizedEntity annotation: " + clazz.getSimpleName(), LogType.OBJECT_CREATION);
			//TODO Error
			return null;
		}
		
		Class<?> identifierClass = null;
		for(Field f : clazz.getDeclaredFields()) {
			if(f.getAnnotation(Identifier.class) != null) {
				if(identifierClass != null) {
					//TODO Error (2 or more identifiers)
					return null;
				}
				identifierClass = f.getClass();
			}
		}
		
		//Check for identifier in superclass
		if(identifierClass == null) {
			if(clazz.getSuperclass() != null && (clazz.getSuperclass().getAnnotation(OrganizedSuperentity.class) != null 
					|| clazz.getSuperclass().getAnnotation(OrganizedEntity.class) != null)) {
				for(Field f : clazz.getSuperclass().getDeclaredFields()) {
					if(f.getAnnotation(Identifier.class) != null) {
						if(identifierClass != null) {
							//TODO Error (2 or more identifiers)
							return null;
						}
						identifierClass = f.getClass();
					}
				}
			}
		}
		
		if(identifierClass == null) {
			LKLogger.getInstance().warn("Class has no identifier: " + clazz.getSimpleName(), LogType.OBJECT_CREATION);
			//TODO Error
			return null;
		}
		
		//Test id identifier is a acceptable class
		if(identifierClass != String.class && identifierClass != UUID.class && !identifierClass.isEnum()) {
			LKLogger.getInstance().warn("Class has wrong identifier class: " + clazz.getSimpleName() + "  "+ identifierClass.getSimpleName(), LogType.OBJECT_CREATION);
			//TODO Error
			return null;
		}
		
		//Test if given identifier matches the identifier class of this object
		if(!identifierClass.isInstance(identifier)) {
			LKLogger.getInstance().warn("Wrong identifier given: " + identifier.getClass().getSimpleName() + "  "+ identifierClass.getSimpleName(), LogType.OBJECT_CREATION);
			//TODO Error
			return null;
		}
		
		//Check if this entity already exists an return it if so
		if(managedEntities.containsKey(clazz)) {
			LKLogger.getInstance().debug("Class exists in managedEntities map: " + clazz.getSimpleName(), LogType.OBJECT_CREATION);
			if(managedEntities.get(clazz).containsKey(identifier)) {
				LKLogger.getInstance().debug("Identifier exists in managedEntities map: " + clazz.getSimpleName() + " , " + identifier, LogType.OBJECT_CREATION);
				return managedEntities.get(clazz).get(identifier);
			}
		} 
		
		//Object does not exist -> Create it
		LKLogger.getInstance().debug("OrgEntity does not yet exist: " + clazz.getSimpleName() + " , " + identifier, LogType.OBJECT_CREATION);
		Object orgEntity = buildOrganizedEntity(clazz, identifier);
		LKLogger.getInstance().debug("OrgEntity created: " + orgEntity, LogType.OBJECT_CREATION);
		
		//Save created object to managedEntity Map
		if(managedEntities.containsKey(clazz)) {
			managedEntities.get(clazz).put(identifier, orgEntity);
		} else {
			Map<Object, Object> map = new HashMap<Object, Object>();
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
	public List<Object> getAllCachedEntities(Class<?> clazz) {
		if(managedEntities.containsKey(clazz)) return new ArrayList<Object>(managedEntities.get(clazz).values());
		return new ArrayList<Object>();
	}
	
	/**
	 * Used to initialize all OrganizedObject fields in a {@link OrganizedSuperentity}.
	 * Calls the constructor of this object after initialization
	 * 
	 * @param clazz The class to build
	 * @param constrValues The values of the non database constructor of this OrganizedSuperentity
	 * @return
	 */
	public Object buildOrganizedSuperentity(Class<?> clazz, Object... constrValues) {
		// Build object with reflection
		try {
			Class<?>[] constrClasses = new Class<?>[constrValues.length];
			int i = 0;
			for(Object o : constrValues) {
				constrClasses[i] = o.getClass();
				i++;
			}
			
			Constructor<?> constr = clazz.getConstructor(constrClasses);
			Object obj = constr.newInstance(constrValues);
			
			for(Field field : obj.getClass().getDeclaredFields()) {
				if(field.getAnnotation(OrganizedObject.class) != null) {
					OrganizedEntity entAnn = clazz.getAnnotation(OrganizedEntity.class);
					OrganizedObject objAnn = field.getAnnotation(OrganizedObject.class); 
				
					DataKey dataKey = new DataKey(entAnn.entityKey(), objAnn.objectKey(), UUID.randomUUID());
					OrganizationType orgType = OrganizationType.NONE;
					
					Constructor<?> fConstr = null;
					if(field.getType() == OrganizedSingleDataObject.class) {
						fConstr = OrganizedSingleDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultDataConverter.class);
					}
					if(field.getType() == OrganizedListDataObject.class) {
						fConstr = OrganizedListDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultDataConverter.class);
					}
					if(field.getType() == OrganizedMapDataObject.class) {
						fConstr = OrganizedMapDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultDataConverter.class);
					}	
					
					DefaultDataConverter<?> conv = null; 

					if(objAnn.listClass() == Object.class && objAnn.mapKeyClass() == Object.class) {
						conv = new DefaultDataConverter<>(objAnn.singleClass());
					} else
						if(objAnn.listClass() != Object.class) {
							conv = new DefaultDataConverter<>(ArrayList.class, objAnn.listClass());
						} else 
							if(objAnn.mapKeyClass() != Object.class) {
								conv = new DefaultDataConverter<>(HashMap.class, objAnn.mapKeyClass(), objAnn.mapValClass());
							}
					
					OrganizedDataObject<?> orgObj = (OrganizedDataObject<?>) fConstr.newInstance(dataKey, orgType, conv);
					field.setAccessible(true);
					field.set(obj, orgObj);
					field.setAccessible(false);
				}
				
				return obj;
			}
		} catch(Exception e) {
			
		}
		
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
	private Object buildOrganizedEntity(Class<?> clazz, Object identifier) {		
		// Build object with reflection
		try {
			Constructor<?> constr = clazz.getConstructor();
			LKLogger.getInstance().debug("OrgEntity creation: Constructor: " + constr, LogType.OBJECT_CREATION);
			
			Object obj = constr.newInstance();
			LKLogger.getInstance().debug("OrgEntity creation: New instance: " + obj, LogType.OBJECT_CREATION);
			
			boolean identifierSet = false;
			for(Field field : obj.getClass().getDeclaredFields()) {
				LKLogger.getInstance().debug("OrgEntity creation: Field found: " + field.getName(), LogType.FIELD_INITIALIZATION);
				if(field.getAnnotation(OrganizedObject.class) != null) {
					LKLogger.getInstance().debug("OrgEntity creation: Field has OrgObj Annotation: " + field.getName(), LogType.FIELD_INITIALIZATION);
					OrganizedEntity entAnn = clazz.getAnnotation(OrganizedEntity.class);
					OrganizedObject objAnn = field.getAnnotation(OrganizedObject.class); 
					LKLogger.getInstance().debug("OrgEntity creation: Annotations: " + entAnn + "   " + objAnn, LogType.FIELD_INITIALIZATION);

					DataKey dataKey = new DataKey(entAnn.entityKey(), objAnn.objectKey(), identifier);
					LKLogger.getInstance().debug("OrgEntity creation: DataKey created: " + dataKey, LogType.FIELD_INITIALIZATION);

					OrganizationType orgType = objAnn.organizationType();
					LKLogger.getInstance().debug("OrgEntity creation: OrganizationType: " + orgType, LogType.FIELD_INITIALIZATION);

					Constructor<?> fConstr = null;
					if(field.getType() == OrganizedSingleDataObject.class) {
						LKLogger.getInstance().debug("OrgEntity creation: OrganizedSingleDataObject", LogType.FIELD_INITIALIZATION);
						fConstr = OrganizedSingleDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultDataConverter.class);
					}
					if(field.getType() == OrganizedListDataObject.class) {
						LKLogger.getInstance().debug("OrgEntity creation: OrganizedListDataObject", LogType.FIELD_INITIALIZATION);
						fConstr = OrganizedListDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultDataConverter.class);
					}
					if(field.getType() == OrganizedMapDataObject.class) {
						LKLogger.getInstance().debug("OrgEntity creation: OrganizedMapDataObject", LogType.FIELD_INITIALIZATION);
						fConstr = OrganizedMapDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultDataConverter.class);
					}	

					LKLogger.getInstance().debug("OrgEntity creation: OrganizedObjectConstr: " + fConstr, LogType.FIELD_INITIALIZATION);

					DefaultDataConverter<?> conv = null; 

					if(objAnn.listClass() == Object.class && objAnn.mapKeyClass() == Object.class) {
						conv = new DefaultDataConverter<>(objAnn.singleClass());
					} else
						if(objAnn.listClass() != Object.class) {
							conv = new DefaultDataConverter<>(ArrayList.class, objAnn.listClass());
						} else 
							if(objAnn.mapKeyClass() != Object.class) {
								conv = new DefaultDataConverter<>(HashMap.class, objAnn.mapKeyClass(), objAnn.mapValClass());
							}

					LKLogger.getInstance().debug("OrgEntity creation: DataConverter: " + conv, LogType.FIELD_INITIALIZATION);

					OrganizedDataObject<?> orgObj = null;
					if(objAnn.defaultBooleanValue() != "" ) {
						fConstr = OrganizedSingleDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultDataConverter.class, Object.class);
						orgObj = (OrganizedDataObject<?>) fConstr.newInstance(dataKey, orgType, conv, Boolean.parseBoolean(objAnn.defaultBooleanValue()));
					} else
					if(objAnn.defaultEnumValue() != "") {
						fConstr = OrganizedSingleDataObject.class.getConstructor(DataKey.class, OrganizationType.class, DefaultDataConverter.class, Object.class);
						if(objAnn.singleClass().isEnum()) {	
							try {
								Field f = objAnn.singleClass().getDeclaredField("$VALUES");
								f.setAccessible(true);
								Enum<?>[] values = (Enum<?>[]) f.get(null);
								for(Enum<?> e : values) {
									if(e.name().equalsIgnoreCase(objAnn.defaultEnumValue())) orgObj = (OrganizedDataObject<?>) fConstr.newInstance(dataKey, orgType, conv, e);
								}
							} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
								e.printStackTrace();
							}
						} else {
							//TODO ERROR
							return null;
						}
						
					} else {
						orgObj = (OrganizedDataObject<?>) fConstr.newInstance(dataKey, orgType, conv);
					}
					 
					field.setAccessible(true);
					field.set(obj, orgObj);
					field.setAccessible(false);
				} 
				
				//Initialize identifier field
				if(field.getAnnotation(Identifier.class) != null) {
					field.setAccessible(true);
					field.set(obj, identifier);
					field.setAccessible(false);
					identifierSet = true;
				}
			}
			//Set identifier in superclass
			if(!identifierSet) {
				for(Field field : obj.getClass().getSuperclass().getDeclaredFields()) {
					//Initialize identifier field
					if(field.getAnnotation(Identifier.class) != null) {
						field.setAccessible(true);
						field.set(obj, identifier);
						field.setAccessible(false);
					}
				}
			}
			
			return obj;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException  
				 e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
}
