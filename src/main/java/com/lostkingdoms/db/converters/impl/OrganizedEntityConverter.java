package com.lostkingdoms.db.converters.impl;

import java.lang.reflect.Field;
import java.util.UUID;

import com.lostkingdoms.db.DataAccessManager;
import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.organization.annotations.Identifier;

/**
 * A converter for {@link OrganizedEntity}s
 * 
 * @author Tim
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
		return (T) DataAccessManager.getInstance().getEntity(getThisClass(), stringToIdentifier(s));
	}

	@Override
	public String convertToDatabase(Object data) {
		for(Field f : data.getClass().getDeclaredFields()) {
			if(f.getAnnotation(Identifier.class) != null) {
				try {
					f.setAccessible(true);
					return identifierToString(f.get(data));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private String identifierToString(Object identifier) {
		Class<?> idClass = identifier.getClass();
		
		if(idClass == UUID.class) return ((UUID)identifier).toString();
		if(idClass == String.class) return (String) identifier;
		if(idClass.isEnum()) return ((Enum<?>)identifier).name();
		return null;
	}
	
	private Object stringToIdentifier(String s) {
		Class<?> idClass = null;
		for(Field f : getThisClass().getDeclaredFields()) {
			if(f.getAnnotation(Identifier.class) != null) idClass = f.getClass();
		}
		if(idClass == UUID.class) return UUID.fromString(s);
		if(idClass == String.class) return s;
		if(idClass.isEnum()) {	
			try {
				Field f = idClass.getDeclaredField("$VALUES");
				f.setAccessible(true);
				Enum<?>[] values = (Enum<?>[]) f.get(null);
				for(Enum<?> e : values) {
					if(e.name().equalsIgnoreCase(s)) return e;
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
}
