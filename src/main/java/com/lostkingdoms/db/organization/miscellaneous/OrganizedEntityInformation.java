package com.lostkingdoms.db.organization.miscellaneous;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.lostkingdoms.db.exceptions.NoIdentifierError;
import com.lostkingdoms.db.exceptions.NoOrganizedEntityException;
import com.lostkingdoms.db.exceptions.NoOrganizedObjectException;
import com.lostkingdoms.db.exceptions.WrongIdentifierClassError;
import com.lostkingdoms.db.exceptions.WrongIdentifierException;
import com.lostkingdoms.db.organization.annotations.Identifier;
import com.lostkingdoms.db.organization.annotations.OrganizedEntity;
import com.lostkingdoms.db.organization.annotations.OrganizedObject;
import com.lostkingdoms.db.organization.annotations.OrganizedSuperentity;

/**
 * Holds all information which are needed for handling a {@link OrganizedEntity}
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
public final class OrganizedEntityInformation {

	private Class<?> clazz;
	
	public OrganizedEntityInformation(Class<?> clazz) throws NoOrganizedEntityException {
		if(clazz.getAnnotation(OrganizedEntity.class) == null &&
				clazz.getAnnotation(OrganizedSuperentity.class) == null)
			throw new NoOrganizedEntityException(clazz);
		this.clazz = clazz;
	}
	
	/**
	 * Gets the class of the {@link OrganizedEntity}s identifier
	 */
	public Class<?> getIdentifierClass() {
		Class<?> currentClass = clazz;
		while(currentClass != null) {
			if(currentClass.getAnnotation(OrganizedEntity.class) != null 
					|| currentClass.getAnnotation(OrganizedSuperentity.class) != null) {
				for(Field f : currentClass.getDeclaredFields()) {
					if(f.getAnnotation(Identifier.class) != null) {
						if(f.getType() != String.class && f.getType() != UUID.class && !f.getType().isEnum())
							throw new WrongIdentifierClassError(currentClass, f.getType());
						
						return f.getType();
					}
				}
			}
			
			if(currentClass.getSuperclass() != null) currentClass = currentClass.getSuperclass();
			else currentClass = null;
		}
		
		throw new NoIdentifierError(clazz);
	}
	
	/**
	 * Gets the class in which the {@link Identifier} field is in
	 */
	public Class<?> getIdentifierEntityClass() {
		Class<?> currentClass = clazz;
		while(currentClass != null) {
			if(currentClass.getAnnotation(OrganizedEntity.class) != null 
					|| currentClass.getAnnotation(OrganizedSuperentity.class) != null) {
				for(Field f : currentClass.getDeclaredFields()) {
					if(f.getAnnotation(Identifier.class) != null) return currentClass;
				}
			}
			
			if(currentClass.getSuperclass() != null) currentClass = currentClass.getSuperclass();
			else currentClass = null;
		}

		throw new NoIdentifierError(clazz);
	}

	/**
	 * Get the {@link Field} of the {@link Identifier}
	 * 
	 * @return 
	 */
	public Field getIdentifierField() {
		Class<?> currentClass = clazz;
		while(currentClass != null) {
			if(currentClass.getAnnotation(OrganizedEntity.class) != null 
					|| currentClass.getAnnotation(OrganizedSuperentity.class) != null) {
				for(Field f : currentClass.getDeclaredFields()) {
					if(f.getAnnotation(Identifier.class) != null) return f;
				}
			}
			
			if(currentClass.getSuperclass() != null) currentClass = currentClass.getSuperclass();
			else currentClass = null;
		}

		throw new NoIdentifierError(clazz);
	}
	
	/**
	 * Get all {@link OrganizedObjectInformation}s from {@link Fields} with the {@link OrganizedObject} annotation in the 
	 * {@link OrganizedEntity} class and all superclasses.
	 * 
	 * @return
	 */
	public List<OrganizedObjectInformation> getOrganizedObjectFields() {
		List<OrganizedObjectInformation> declaredFields = new ArrayList<>();
		
		Class<?> currentClass = clazz;
		while(currentClass != null) {
			if(currentClass.getAnnotation(OrganizedEntity.class) != null 
					|| currentClass.getAnnotation(OrganizedSuperentity.class) != null) {
				for(Field f : currentClass.getDeclaredFields()) {
					if(f.getAnnotation(OrganizedObject.class) != null)
						try {
							declaredFields.add(new OrganizedObjectInformation(f, clazz));
						} catch (NoOrganizedObjectException e) {
							e.printStackTrace();
						}
				}
			}
			
			if(currentClass.getSuperclass() != null) currentClass = currentClass.getSuperclass();
			else currentClass = null;
		}
		
		return declaredFields;
	}
	
	/**
	 * Gets the entitityKey for a {@link OrganizedEntity}
	 * 
	 * @return key from annotation or lower case class name
	 */
	public String getEntityKey() {
		OrganizedEntity orgEnt = clazz.getAnnotation(OrganizedEntity.class);
		if(orgEnt != null && orgEnt.entityKey().equals("")) {
			return orgEnt.entityKey();
		}
		return clazz.getSimpleName().toLowerCase();
	}
	
	/**
	 * Converts an identifier for this object to it's string representation
	 * 
	 * @param identifier
	 * @return
	 * @throws WrongIdentifierException
	 */
	public String identifierToString(Object identifier) throws WrongIdentifierException {
		Class<?> idClass = getIdentifierClass();
		if(idClass != identifier.getClass()) throw new WrongIdentifierException(clazz, identifier.getClass());
		
		if(idClass == UUID.class) return ((UUID)identifier).toString();
		if(idClass == String.class) return (String) identifier;
		if(idClass.isEnum()) return ((Enum<?>)identifier).name();
		return null;
	}
	
	/**
	 * Converts a string representation of an identifier of this object back to an object
	 * 
	 * @param identifierString
	 * @return
	 */
	public Object stringToIdentifier(String identifierString) {
		Class<?> idClass = getIdentifierClass();
		if(idClass == UUID.class) return UUID.fromString(identifierString.replace("\"", ""));
		if(idClass == String.class) return identifierString.replace("\"", "");
		if(idClass.isEnum()) {	
			try {
				Field f = null;
				try {
					f = idClass.getDeclaredField("ENUM$VALUES");
				} catch(NoSuchFieldException e) {
					f = idClass.getDeclaredField("$VALUES");
				}
				
				f.setAccessible(true);
				Enum<?>[] values = (Enum<?>[]) f.get(null);
				for(Enum<?> e : values) {
					if(e.name().equalsIgnoreCase(identifierString.replace("\"", ""))) return e;
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	
		return null;
	}
	
}
