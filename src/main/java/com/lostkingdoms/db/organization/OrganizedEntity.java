package com.lostkingdoms.db.organization;

import java.util.UUID;

/**
 * Abstract base class which all OrganizedEntities must extend from
 * 
 * @author Tim
 *
 */
public abstract class OrganizedEntity {

	/**
	 * The identifier to identify this {@link OrganizedEntity} in the database
	 */
	UUID identifier;
	
	public OrganizedEntity(UUID identifier) {
		this.identifier = identifier;
	}
	
	/**
	 * Get this {@link OrganizedEntity}s ID
	 * 
	 * @return
	 */
	public UUID getID() {
		return this.identifier;
	}
	
}
