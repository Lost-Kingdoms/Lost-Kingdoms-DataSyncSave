package com.lostkingdoms.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.UUID;

import com.lostkingdoms.db.exceptions.IllegalIdentifierClassException;
import com.lostkingdoms.db.exceptions.MissingOrganizedEntityKeyException;
import com.lostkingdoms.db.exceptions.MissingOrganizedObjectKeyException;
import com.lostkingdoms.db.exceptions.MissingOrganizedObjectTypeException;
import com.lostkingdoms.db.jedis.JedisFactory;

import redis.clients.jedis.Jedis;

public class DataAccessObject {
	
	public static Object getOrganizedEntity(Class<?> clazz, UUID identifier) {
		return buildOrganizedEntity(clazz, identifier, OrganizedDataType.MUTABLE);
	}
	
	public static Object getOrganizedEntity(Class<?> clazz, UUID identifier,  OrganizedDataType organizedDataType) {
		return buildOrganizedEntity(clazz, identifier, organizedDataType);
	}
	
	public static MutableOrganizedDataObject<?> getAllOrganizedEntites(Class<?> clazz) {
		
	}
	
	public static OrganizedDataObject<?> getAllOrganizedEntities(Class<?> clazz, OrganizedDataType organizedDataType) {
		
	}
	
	public static MutableOrganizedDataObject<?> getOrganizedDataObject(Class<?> clazz, String fieldKey) {
		
	}
	
	public static OrganizedDataObject<?> getOrganizedDataObject(Class<?> clazz, String fieldKey, OrganizedDataType organizedDataType) {
		
	}
	
	public static void save(OrganizedDataObject<?> dataObject) {
		
	}
	
	public static void remove(OrganizedDataObject<?> dataObject) {
		
	}
	
	/**
	 * Builds an OrgnizedEntity object for the given identifier.
	 * DOES NOT CHECK the existence of the identifier in global cache or db
	 * 
	 * @param clazz
	 * @param identifier
	 * @param organizedDataType
	 * @return builded object or null if clazz is no OrganizedEntity
	 */
	private static Object buildOrganizedEntity(Class<?> clazz, UUID identifier, OrganizedDataType organizedDataType) {
		// Check if requested class is OrganizedEntity
		
		if(clazz.getAnnotation(OrganizedEntity.class) == null) return null;
		
		// Build object with reflection
		
		try {
			Constructor<?> constr = clazz.getConstructor(UUID.class);

			Object obj = constr.newInstance(identifier);
			
			for(Field field : obj.getClass().getDeclaredFields()) {
				if(field.getAnnotation(OrganizedObject.class) != null) {
					OrganizedEntity entAnn = clazz.getAnnotation(OrganizedEntity.class);
					OrganizedObject objAnn = field.getAnnotation(OrganizedObject.class); 
					
					if(entAnn.key() != null && objAnn.key() != null) {
						DataKey dataKey = new DataKey(entAnn.key(), objAnn.key(), identifier);
						
						if(objAnn.organizationType() == null) throw new MissingOrganizedObjectTypeException(clazz.getSimpleName() + "." + field.getName());
						OrganizationType orgType = objAnn.organizationType();
						
						if(organizedDataType == OrganizedDataType.MUTABLE) {
							Constructor<?> fConstr = MutableOrganizedDataObject.class.getConstructor(DataKey.class, OrganizationType.class);
							
							MutableOrganizedDataObject<?> orgObj = (MutableOrganizedDataObject<?>) fConstr.newInstance(dataKey, orgType);
							field.setAccessible(true);
							field.set(obj, orgObj);
							field.setAccessible(false);
						}
						if(organizedDataType == OrganizedDataType.IMMUTABLE) {
							Constructor<?> fConstr = ImmutableOrganizedDataObject.class.getConstructor(DataKey.class);
							
							ImmutableOrganizedDataObject<?> orgObj = (ImmutableOrganizedDataObject<?>) fConstr.newInstance(dataKey);
							field.setAccessible(true);
							field.set(obj, orgObj);
							field.setAccessible(false);
						}
						
					} else {
						if(entAnn.key() == null) {
							throw new MissingOrganizedEntityKeyException(clazz.getSimpleName());
						}
						if(objAnn.key() == null) {
							throw new MissingOrganizedObjectKeyException(clazz.getSimpleName() + "." + field.getName());
						}
					}
					
				} else if(field.getAnnotation(Identifier.class) != null) {
					if(field.getType() == UUID.class) {
						field.setAccessible(true);
						field.set(obj, identifier);
						field.setAccessible(false);
					} else {
						throw new IllegalIdentifierClassException(field.getType().getSimpleName());
					}
				}
			}
			
			return obj;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException | IllegalIdentifierClassException 
				| MissingOrganizedEntityKeyException | MissingOrganizedObjectKeyException | MissingOrganizedObjectTypeException e) {
			e.printStackTrace();
		} 
		
		return null;
		
	}
	
}
