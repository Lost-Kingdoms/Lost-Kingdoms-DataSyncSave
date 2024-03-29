package com.lostkingdoms.db.converters.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.organization.objects.OrganizedSingleDataObject;

/**
 * The default data converter for {@link OrganizedSingleDataObject}
 * 
 * @author Tim K�chler (https://github.com/TimK1998)
 *
 * @param <T>
 */
public final class DefaultDataConverter<T> {

	/**
	 * The generic class
	 */
	private Class<T> genericClass;

	
	/**
	 * Constructor for non-map and non-list {@link DefaultDataConverter}
	 * 
	 * @param genericClass
	 */
	public DefaultDataConverter(Class<T> genericClass) {
		this.genericClass = genericClass;
	}

	@SuppressWarnings({ "unchecked"})
	public T convertFromDatabase(String s) {
		if(s == null) return null;
		
		//The Gson instance
		Gson gson = new Gson();

		//Check if there is a converter assigned for class T
		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		if(dataOrganizationManager.hasDataConverter(this.genericClass)) {
			return ((AbstractDataConverter<T>)dataOrganizationManager.getDataConverter(this.genericClass)).convertFromDatabase(s);
		}
	
		TypeToken<T> token = TypeToken.of(this.genericClass);
		return gson.fromJson(s, token.getType());
	}

	@SuppressWarnings("unchecked")
	public String convertToDatabase(T t) {
		if(t == null) return "";
		
		Object data = t;

		//The Gson instance
		Gson gson = new Gson();

		//Check if there is a converter assigned for class T
		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		if(dataOrganizationManager.hasDataConverter(t.getClass())) {
			data = ((AbstractDataConverter<T>)dataOrganizationManager.getDataConverter(t.getClass())).convertToDatabase(t);
		}

		return gson.toJson(data);
	}

}
