package com.lostkingdoms.db.converters.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.converters.AbstractDataConverter;

public class DefaultListDataConverter<T> {

	Class<T> genericClass;
	
	/**
	 * Constructor
	 * 
	 * @param thisClass
	 */
	public DefaultListDataConverter(Class<T> genericClass) {
		this.genericClass = genericClass;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public ArrayList<T> convertFromDatabase(String s) {
		//The Gson instance
		Gson gson = new Gson();
		
		//Manager
		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		
		//Convert object from json
		Object obj = null;
		
		if(dataOrganizationManager.hasDataConverter(this.genericClass)) {
			obj = gson.fromJson(s, new TypeToken<ArrayList<String>>() {}.getType());
		} else {
			obj = gson.fromJson(s, new TypeToken<ArrayList<T>>() {}.getType());
		}

		ArrayList<T> newList = new ArrayList<T>();

		if(dataOrganizationManager.hasDataConverter(this.genericClass)) {
			for(String o : ((List<String>)obj)) {
				AbstractDataConverter<?> converter = dataOrganizationManager.getDataConverter(this.genericClass);
				newList.add((T) converter.convertFromDatabase(o));
			}
		}

		if(newList.size() > 0) return newList;


		 return (ArrayList<T>) obj;
	}

	public String convertToDatabase(ArrayList<T> t) {
		//The Gson instance
		Gson gson = new Gson();

		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		
		Object data = t;
		//Check if list elements have registered converter
		if(t.size() != 0) {
			if(dataOrganizationManager.hasDataConverter(t.get(0).getClass())) {
				List<String> newList = new ArrayList<String>();

				AbstractDataConverter<?> converter = dataOrganizationManager.getDataConverter(t.get(0).getClass());
				//Convert list elements one by one and add them to a new list
				for(T o : t) {
					newList.add(converter.convertToDatabase(o));
				}

				data = newList;
			}
		}

		return gson.toJson(data);
	}
}
