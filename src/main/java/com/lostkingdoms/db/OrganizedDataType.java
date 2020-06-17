package com.lostkingdoms.db;

/**
 * Is used to indicate if {@link DataAccessObject} should retrieve {@link OrganizedDataObject} as
 * {@link ImmutableOrganizedDataObject} or {@link MutableOrganizedDataObject}
 */
public enum OrganizedDataType {

	MUTABLE, IMMUTABLE;
	
}
