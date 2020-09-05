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

	/**
	 * The generic class of the first map type argument or the list type
	 */
	private Class<?> genericClass1;
	
	/**
	 * The generic class of the second map type argument
	 */
	private Class<?> genericClass2;
	
	
	
	/**
	 * Constructor for non-map and non-list {@link DefaultDataConverter}
	 * 
	 * @param thisClass
	 */
	public DefaultDataConverter(Class<T> thisClass) {
		super(thisClass);
	}
	
	/**
	 * Constructor for list {@link DefaultDataConverter}
	 * 
	 * @param thisClass
	 * @param genericClass1
	 */
	public DefaultDataConverter(Class<T> thisClass, Class<?> genericClass) {
		super(thisClass);
		this.genericClass1 = genericClass;
	}
	
	/**
	 * Constructor for map {@link DefaultDataConverter}
	 * 
	 * @param thisClass
	 * @param genericClass1
	 * @param genericClass2
	 */
	public DefaultDataConverter(Class<T> thisClass, Class<?> genericClass1, Class<?> genericClass2) {
		super(thisClass);
		this.genericClass1 = genericClass1;
		this.genericClass2 = genericClass2;
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
		
		//Convert object from json
		Object obj = null;
		try {
			obj = gson.fromJson(s, getThisClass());
		} catch (Exception e) {
			if(genericClass2 != null) {
				Map<String, String> temp = new HashMap<String, String>();
				obj = gson.fromJson(s, temp.getClass());
			} else {
				List<String> temp = new ArrayList<String>();
				obj = gson.fromJson(s, temp.getClass());
			}
		}
		
		//Object is list
		if(obj instanceof List) {
			List<Object> newList = new ArrayList<Object>();
			
			for(String o : ((List<String>)obj)) {
				if(dataOrganizationManager.hasDataConverter(this.genericClass1)) {
					AbstractDataConverter<?> converter = dataOrganizationManager.getDataConverter(this.genericClass1);
					newList.add(converter.convertFromDatabase(o));
				}
			}
			
			if(newList.size() > 0) return (T) ((Object) newList);
		}

		//Object is map
		if(obj instanceof Map) {
			Map<Object, Object> newMap = new HashMap<Object, Object>();
			
			for(Entry<String, String> entry: ((Map<String, String>)obj).entrySet()) {
				if(dataOrganizationManager.hasDataConverter(genericClass1) &&
						dataOrganizationManager.hasDataConverter(genericClass2)) {
					newMap.put(dataOrganizationManager.getDataConverter(genericClass1).convertFromDatabase(entry.getKey())
							, dataOrganizationManager.getDataConverter(genericClass2).convertFromDatabase(entry.getValue()));
				} else
				if(dataOrganizationManager.hasDataConverter(genericClass1)) {
					newMap.put(dataOrganizationManager.getDataConverter(genericClass1).convertFromDatabase(entry.getKey())
							, gson.fromJson(entry.getValue(), genericClass2));
				} else
				if(dataOrganizationManager.hasDataConverter(genericClass2)){
					newMap.put(gson.fromJson(entry.getKey(), genericClass1)
							, dataOrganizationManager.getDataConverter(genericClass2).convertFromDatabase(entry.getValue()));
				}
			}
			
			if(newMap.size() > 0) return (T) ((Object) newMap);
		}

		 return (T) obj;

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
