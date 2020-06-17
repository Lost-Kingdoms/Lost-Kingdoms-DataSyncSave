package com.lostkingdoms.db;

import java.util.UUID;

public class DataAccessObject {
	
	public static MutableOrganizedDataObject<?> getOrganizedEntity(Class<?> clazz, UUID identifier) {
		
	}
	
	public static OrganizedDataObject<?> getOrganizedEntity(Class<?> clazz, UUID identifier,  OrganizedDataType organizedDataType) {
		
	}
	
	public static MutableOrganizedDataObject<?> getAllOrganizedEntites(Class<?> clazz) {
		
	}
	
	public static OrganizedDataObject<?> getAllOrganizedEntities(Class<?> clazz, OrganizedDataType organizedDataType) {
		
	}
	
	public static MutableOrganizedDataObject<?> getOrganizedDataObject(Class<?> clazz, String fieldKey) {
		
	}
	
	public static OrganizedDataObject<?> getOrganizedDataObject(Class<?> clazz, String fieldKey, OrganizedDataType organizedDataType) {
		
	}
	
	public static void save(OrganizedDataObject<?> dataObject) {
		
	}
	
	public static void remove(OrganizedDataObject<?> dataObject) {
		
	}
	
}
