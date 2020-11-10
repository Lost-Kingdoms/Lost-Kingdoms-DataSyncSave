package com.lostkingdoms.db.converters.impl;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.lostkingdoms.db.DataOrganizationManager;
import com.lostkingdoms.db.organization.objects.OrganizedMapDataObject;

/**
 * Default data converter for {@link OrganizedMapDataObject}
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 * @param <K> key
 * @param <V> value
 */
public class DefaultMapDataConverter<K, V> {

	/**
	 * The generic class of the first map type argument or the list type
	 */
	private Class<K> genericClass1;
	
	/**
	 * The generic class of the second map type argument
	 */
	private Class<V> genericClass2;
	
	
	
	/**
	 * Constructor
	 * 
	 * @param thisClass
	 */
	public DefaultMapDataConverter(Class<K> genericClass1, Class<V> genericClass2) {
		this.genericClass1 = genericClass1;
		this.genericClass2 = genericClass2;
	}
	

	@SuppressWarnings({ "unchecked", "serial" })
	public HashMap<K, V> convertFromDatabase(String s) {
		//The Gson instance
		Gson gson = new Gson();

		//Manager
		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		
		//Convert object from json
		Object obj = null;

		if(dataOrganizationManager.hasDataConverter(this.genericClass1) && dataOrganizationManager.hasDataConverter(this.genericClass2)) {
			obj = gson.fromJson(s, new TypeToken<HashMap<String, String>>() {}.getType());
		}
		else if(dataOrganizationManager.hasDataConverter(this.genericClass1)) {
			TypeToken<V> tokenV = TypeToken.of(this.genericClass2);
			Type type = new TypeToken<HashMap<String, V>>() {}
			.where(new TypeParameter<V>() {}, tokenV)
			.getType();
			obj = gson.fromJson(s, type);
		}
		else if(dataOrganizationManager.hasDataConverter(this.genericClass2)) {
			TypeToken<K> tokenK = TypeToken.of(this.genericClass1);
			Type type = new TypeToken<HashMap<K, String>>() {}
			.where(new TypeParameter<K>() {}, tokenK)
			.getType();
			obj = gson.fromJson(s, type);
		} 
		else {
			TypeToken<K> tokenK = TypeToken.of(this.genericClass1);
			TypeToken<V> tokenV = TypeToken.of(this.genericClass2);
			Type type = new TypeToken<HashMap<K, V>>() {}
				.where(new TypeParameter<K>() {}, tokenK)
				.where(new TypeParameter<V>() {}, tokenV)
				.getType();
			obj = gson.fromJson(s, type);
		}
	
		HashMap<K, V> newMap = new HashMap<>();

		if(dataOrganizationManager.hasDataConverter(this.genericClass1) && dataOrganizationManager.hasDataConverter(this.genericClass2)) {
			for(Entry<String, String> entry: ((Map<String, String>)obj).entrySet()) {
				newMap.put((K) dataOrganizationManager.getDataConverter(genericClass1).convertFromDatabase(entry.getKey())
						, (V) dataOrganizationManager.getDataConverter(genericClass2).convertFromDatabase(entry.getValue()));
			}
		}
		else if(dataOrganizationManager.hasDataConverter(this.genericClass1)) {
			for(Entry<String, V> entry: ((Map<String, V>)obj).entrySet()) {
				newMap.put((K) dataOrganizationManager.getDataConverter(genericClass1).convertFromDatabase(entry.getKey())
						, entry.getValue());
			}
		}
		else if(dataOrganizationManager.hasDataConverter(this.genericClass2)) {
			for(Entry<K, String> entry: ((Map<K, String>)obj).entrySet()) {
				newMap.put(entry.getKey()
						, (V) dataOrganizationManager.getDataConverter(genericClass2).convertFromDatabase(entry.getValue()));
			}
		} 
		
		if(newMap.size() > 0) return newMap;

		return (HashMap<K, V>) obj;

	}

	public String convertToDatabase(HashMap<K, V> map) {
		//The Gson instance
		Gson gson = new Gson();

		//Manager
		DataOrganizationManager dataOrganizationManager = DataOrganizationManager.getInstance();
		
		
		Object data = map;
		//Data is map
		//If Objects in Map are OrganizedEntities convert them to it's identifier 
		//Otherwise do nothing
		Map<Object, Object> newMap = new HashMap<>();

		//Check every map element one by one and convert it
		for(Entry<K, V> entry : map.entrySet()) {
			if(dataOrganizationManager.hasDataConverter(entry.getKey().getClass())
					&& dataOrganizationManager.hasDataConverter(entry.getValue().getClass())) {
				newMap.put(dataOrganizationManager.getDataConverter(entry.getKey().getClass()).convertToDatabase(entry.getKey())
						, dataOrganizationManager.getDataConverter(entry.getValue().getClass()).convertToDatabase(entry.getValue()));
			} 
			else if(dataOrganizationManager.hasDataConverter(entry.getKey().getClass())) {
				newMap.put(dataOrganizationManager.getDataConverter(entry.getKey().getClass()).convertToDatabase(entry.getKey())
							, entry.getValue());
			} 
			else if(dataOrganizationManager.hasDataConverter(entry.getValue().getClass())) {
					newMap.put(entry.getKey()
								, dataOrganizationManager.getDataConverter(entry.getValue().getClass()).convertToDatabase(entry.getValue()));
			} 
			else {
				break;
				}
		}

		if(newMap.size() > 0) data = newMap;

		return gson.toJson(data);
	}
	
}
