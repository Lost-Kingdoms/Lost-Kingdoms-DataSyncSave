package com.lostkingdoms.db.converters.impl;

import java.lang.reflect.Field;

import com.lostkingdoms.db.DataAccessManager;
import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.exceptions.NoOrganizedEntityException;
import com.lostkingdoms.db.exceptions.WrongIdentifierException;
import com.lostkingdoms.db.organization.annotations.OrganizedEntity;
import com.lostkingdoms.db.organization.miscellaneous.OrganizedEntityInformation;

/**
 * A converter for {@link OrganizedEntity}s
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 * @param <T>
 */
public final class OrganizedEntityConverter<T> extends AbstractDataConverter<T> {

	public OrganizedEntityConverter(Class<T> thisClass) {
		super(thisClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T convertFromDatabase(String s) {
		try {
			OrganizedEntityInformation info = new OrganizedEntityInformation(getThisClass());
			return (T) DataAccessManager.getInstance().getEntity(getThisClass(), info.stringToIdentifier(s));
		} catch (NoOrganizedEntityException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String convertToDatabase(Object data) {
		if(data == null) return "";
		try {
			OrganizedEntityInformation info = new OrganizedEntityInformation(getThisClass());
			
			Field f = info.getIdentifierField();
			f.setAccessible(true);
			return info.identifierToString(f.get(data));
		} catch (NoOrganizedEntityException | IllegalArgumentException 
				| IllegalAccessException | WrongIdentifierException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}
