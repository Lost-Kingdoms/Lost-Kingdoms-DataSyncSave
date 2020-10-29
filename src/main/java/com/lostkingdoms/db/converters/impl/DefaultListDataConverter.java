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
 * @author Tim K�chler (https://github.com/TimK1998)
 *
 * @param <T>
 */
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
			TypeToken<T> token = TypeToken.of(this.genericClass);
			Type type = new TypeToken<ArrayList<T>>() {}
				.where(new TypeParameter<T>() {}, token)
				.getType();
			obj = gson.fromJson(s, type);
		}

		ArrayList<T> newList = new ArrayList<T>();

		if(dataOrganizationManager.hasDataConverter(this.genericClass)) {
			for(String o : ((List<String>)obj)) {
				String[] sub = o.split(":");
				
				AbstractDataConverter<?> converter = null;
				if(sub.length != 1) {
					try {
						converter = dataOrganizationManager.getDataConverter(Class.forName(sub[1]));
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				} else {
					converter = dataOrganizationManager.getDataConverter(this.genericClass);
				}
				
				newList.add((T) converter.convertFromDatabase(sub[0]));
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

				
				//Convert list elements one by one and add them to a new list
				for(T o : t) {
					AbstractDataConverter<?> converter = dataOrganizationManager.getDataConverter(o.getClass());
					
					Class<?> clazz = o.getClass();
					boolean superList = false;
					while(clazz.getSuperclass() != null) {
						if(clazz.getSuperclass() == this.genericClass) {
							superList = true;
						}
						clazz = clazz.getSuperclass();
					}
					if(superList) {
						newList.add(converter.convertToDatabase(o) + ":" + o.getClass().getName());
					} else {
						newList.add(converter.convertToDatabase(o));
					}	
				}

				data = newList;
			}
		}

		return gson.toJson(data);
	}
}
