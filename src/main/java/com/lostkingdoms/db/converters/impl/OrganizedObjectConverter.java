package com.lostkingdoms.db.converters.impl;

import java.util.UUID;

import com.lostkingdoms.db.DataAccessManager;
import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.organization.OrganizedEntity;

/**
 * A converter for {@link OrganizedEntity}s
 * 
 * @author Tim
 *
 * @param <T>
 */
public final class OrganizedObjectConverter<T extends OrganizedEntity> extends AbstractDataConverter<T> {

	public OrganizedObjectConverter(Class<T> thisClass) {
		super(thisClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T convertFromDatabase(String s) {
		UUID id = UUID.fromString(s);
		return (T) DataAccessManager.getInstance().getEntity(getThisClass(), id);
	}

	@Override
	public String convertToDatabase(Object data) {
		if(!(data instanceof OrganizedEntity)) return null;
		
		return ((OrganizedEntity)data).getID().toString();
	}

}
