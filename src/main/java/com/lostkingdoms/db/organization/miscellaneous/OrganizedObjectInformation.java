package com.lostkingdoms.db.organization.miscellaneous;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.lostkingdoms.db.exceptions.NoOrganizedObjectException;
import com.lostkingdoms.db.exceptions.WrongMethodUseException;
import com.lostkingdoms.db.organization.annotations.OrganizedEntity;
import com.lostkingdoms.db.organization.annotations.OrganizedObject;
import com.lostkingdoms.db.organization.annotations.OrganizedSuperentity;
import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.objects.OrganizedListDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedMapDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedSingleDataObject;

/**
 * Holds all information which are needed for handling a {@link OrganizedEntity}
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
public final class OrganizedObjectInformation {

	/**
	 * The {@link Field} which is represented
	 */
	private Field objectField;
	
	/**
	 * The {@link OrganizedEntity} class this field is declared in
	 */
	private Class<?> buildClass;
	
	public OrganizedObjectInformation(Field objectField, Class<?> buildClass) throws NoOrganizedObjectException {
		if(objectField.getAnnotation(OrganizedObject.class) == null)
			throw new NoOrganizedObjectException(objectField, buildClass);
		this.objectField = objectField;
		this.buildClass = buildClass;
	}
	
	/**
	 * Get the {@link Field} 
	 * 
	 * @return
	 */
	public Field getField() {
		return objectField;
	}
	
	/**
	 * Gets the objectKey for a {@link OrganizedObject}
	 * 
	 * @return key from annotation or lower case field name
	 */
	public String getObjectKey() {
		OrganizedObject objAnn = objectField.getAnnotation(OrganizedObject.class);
		
		if(objAnn.objectKey() != "") return objAnn.objectKey();
		return objectField.getName().toLowerCase();
	}
	
	/**
	 * Gets the {@link OrganizationType} 
	 * 
	 * @return If build class is {@link OrganizedEntity} returns annotated or default {@link OrganizationType}.
	 * If build class is {@link OrganizedSuperentity} returns NONE
	 */
	public OrganizationType getOrganizationType() {
		OrganizedObject objAnn = objectField.getAnnotation(OrganizedObject.class);
		
		if(buildClass.getAnnotation(OrganizedEntity.class) != null) 
			return objAnn.organizationType();

		return OrganizationType.NONE;
	}
	
	/**
	 * Gets the class of this {@link OrganizedObject}.
	 * ({@link OrganizedSingleDataObject}, {@link OrganizedListDataObject}
	 * or {@link OrganizedMapDataObject})
	 * 
	 * @return
	 */
	public Class<?> getDataObjectClass() {
		return objectField.getType();
	}
	
	/**
	 * Gets the single class of this {@link OrganizedObject}
	 * 
	 * @return
	 */
	public Class<?> getSingleClass() {
		if(getDataObjectClass() == OrganizedSingleDataObject.class) 
			return objectField.getAnnotation(OrganizedObject.class).singleClass();
		if(getDataObjectClass() == OrganizedListDataObject.class) 
			return ArrayList.class;
		return HashMap.class;
	}
	
	/**
	 * Gets the list class for {@link OrganizedListDataObject}
	 * 
	 * @return
	 * @throws WrongMethodUseException
	 */
	public Class<?> getListClass() throws WrongMethodUseException {
		if(getDataObjectClass() == OrganizedSingleDataObject.class 
				|| getDataObjectClass() == OrganizedMapDataObject.class) 
			throw new WrongMethodUseException("getListClass()", getDataObjectClass());
		
		return objectField.getAnnotation(OrganizedObject.class).listClass();
	}
	
	/**
	 * Gets the key and value class for {@link OrganizedMapDataObject}
	 * 
	 * @return Pair: L = keyClass, R = valueClass
	 * @throws WrongMethodUseException
	 */
	public Pair<Class<?>, Class<?>> getMapClass() throws WrongMethodUseException {
		if(getDataObjectClass() == OrganizedSingleDataObject.class 
				|| getDataObjectClass() == OrganizedListDataObject.class) 
			throw new WrongMethodUseException("getListClass()", getDataObjectClass());
		
		OrganizedObject objAnn = objectField.getAnnotation(OrganizedObject.class);
		return new ImmutablePair<Class<?>, Class<?>>(objAnn.mapKeyClass(), objAnn.mapValClass());
	}
	
}
