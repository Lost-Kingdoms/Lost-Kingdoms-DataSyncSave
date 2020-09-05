package com.lostkingdoms.db.converters.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.lostkingdoms.db.converters.AbstractDataConverter;
import com.lostkingdoms.db.organization.DataOrganizationManager;

/**
 * The default data converter.
 * 
 * @author Tim
 *
 * @param <T>
 */
public final class DefaultDataConverter<T> extends AbstractDataConverter<T> {

	public DefaultDataConverter(Class<T> thisClass) {
		super(thisClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T convertFromDatabase(String s) {
		//The Gson instance
		Gson gson = new Gson();

		//Check if there is a converter assigned for class T
		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		if(dataOrganizationManager.hasDataConverter(getThisClass())) {
			return ((AbstractDataConverter<T>)dataOrganizationManager.getDataConverter(getThisClass())).convertFromDatabase(s);
		}

		//TODO if List or map contains OrganizedObjects

		return gson.fromJson(s, getThisClass());

	}

	@SuppressWarnings("unchecked")
	@Override
	public String convertToDatabase(Object t) {
		//Check if given object t is  the correct class for this converter
		if(!getThisClass().isInstance(t)) return null;

		Object data = t;

		//The Gson instance
		Gson gson = new Gson();

		//Check if there is a converter assigned for class T
		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		if(dataOrganizationManager.hasDataConverter(t.getClass())) {
			data = ((AbstractDataConverter<T>)dataOrganizationManager.getDataConverter(t.getClass())).convertToDatabase(t);
		}

		//Data is a list
		//If Objects in List have a registered converter, convert them with this converter
		//Otherwise do nothing
		if(data instanceof List<?>) {
			//Check if list elements have registered converter
			if(dataOrganizationManager.hasDataConverter(((List<?>)data).get(0).getClass())) {
				List<String> newList = new ArrayList<String>();

				AbstractDataConverter<?> converter = dataOrganizationManager.getDataConverter(((List<?>)data).get(0).getClass());
				//Convert list elements one by one and add them to a new list
				for(Object o : ((List<?>)data)) {
					newList.add(converter.convertToDatabase(o));
				}

				data = newList;
			}
		}

		//Data is map
		//If Objects in Map are OrganizedEntities convert them to it's identifier 
		//Otherwise do nothing
		if(data instanceof Map<?, ?>) {
			Map<String, String> newMap = new HashMap<String, String>();
			
			//Check every map element one by one and convert it
			for(Entry<?, ?> entry : ((Map<?, ?>)data).entrySet()) {
				if(dataOrganizationManager.hasDataConverter(entry.getKey().getClass())
						&& dataOrganizationManager.hasDataConverter(entry.getValue().getClass())) {
					newMap.put(dataOrganizationManager.getDataConverter(entry.getKey().getClass()).convertToDatabase(entry.getKey())
							, dataOrganizationManager.getDataConverter(entry.getValue().getClass()).convertToDatabase(entry.getValue()));
				} else
				if(dataOrganizationManager.hasDataConverter(entry.getKey().getClass())) {
					newMap.put(dataOrganizationManager.getDataConverter(entry.getKey().getClass()).convertToDatabase(entry.getKey())
							, gson.toJson(entry.getValue()));
				} else
				if(dataOrganizationManager.hasDataConverter(entry.getValue().getClass())) {
					newMap.put(gson.toJson(entry.getKey())
							, dataOrganizationManager.getDataConverter(entry.getValue().getClass()).convertToDatabase(entry.getValue()));
				} else {
					break;
				}
			}

			if(newMap.size() > 0) data = newMap;

		}
		
		return gson.toJson(data);
	}

	//	private <S> Type getType(Class<S> type) {
	//        Type typeOfObjectsListNew = new TypeToken<ArrayList<S>>() {}.getType();
	//        return typeOfObjectsListNew;
	//    }

}
