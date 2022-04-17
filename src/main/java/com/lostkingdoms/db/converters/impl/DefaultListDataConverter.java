package com.lostkingdoms.db.converters.impl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.organization.objects.OrganizedListDataObject;

/**
 * The default data converter for {@link OrganizedListDataObject}
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 * @param <T>
 */
public class DefaultListDataConverter<T> {

	Class<T> genericClass;

	/**
	 * Constructor
	 * 
	 * @param genericClass
	 */
	public DefaultListDataConverter(Class<T> genericClass) {
		this.genericClass = genericClass;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public List<T> convertFromDatabase(String s) {
		//The Gson instance
		Gson gson = new Gson();
		
		//Manager
		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		
		//Convert object from json
		Object obj = null;
		
		
		if(dataOrganizationManager.hasDataConverter(this.genericClass)) {
			obj = gson.fromJson(s, new TypeToken<ArrayList<String>>() {}.getType());
		} else {
			TypeToken<T> token = TypeToken.of(this.genericClass);
			Type type = new TypeToken<ArrayList<T>>() {}
				.where(new TypeParameter<T>() {}, token)
				.getType();
			obj = gson.fromJson(s, type);
		}

		ArrayList<T> newList = new ArrayList<>();
		if(dataOrganizationManager.hasDataConverter(this.genericClass)) {
			if (obj != null) {
				for (String o : ((List<String>) obj)) {
					String[] sub = o.split(":");

					AbstractDataConverter<?> converter = null;
					if (sub.length != 1) {
						try {
							converter = dataOrganizationManager.getDataConverter(Class.forName(sub[sub.length-1]));
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					} else {
						converter = dataOrganizationManager.getDataConverter(this.genericClass);
					}

					//TODO Remove on new version
					if (this.genericClass.getSimpleName().toUpperCase().contains("LOCATION")) {
						newList.add((T) converter.convertFromDatabase("" + sub[0] + ":" + sub[1] + ":" + sub[2] + ":" + sub[3]));
					} else {
						newList.add((T) converter.convertFromDatabase(sub[0]));
					}
				}
			}
		}

		if(!newList.isEmpty()) return newList;
		return (ArrayList<T>) obj;
	}

	public String convertToDatabase(List<T> t) {
		//The Gson instance
		Gson gson = new Gson();

		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		
		Object data = t;
		//Check if list elements have registered converter
		if(!t.isEmpty() && dataOrganizationManager.hasDataConverter(t.get(0).getClass())) {
			List<String> newList = new ArrayList<>();

			//Convert list elements one by one and add them to a new list
			for(T o : t) {
				AbstractDataConverter<?> converter = dataOrganizationManager.getDataConverter(o.getClass());

				newList.add(converter.convertToDatabase(o) + ":" + o.getClass().getName());
			}

			data = newList;
		}

		return gson.toJson(data);
	}
}
