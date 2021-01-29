package com.lostkingdoms.db.organization.enums;

import com.lostkingdoms.db.organization.objects.OrganizedSingleDataObject;

/**
 * Defines the tasks for an {@link OrganizedSingleDataObject}.
 * SYNC: Save to redis global cache
 * SAVE_TO_DB: Save to mongoDB database
 * BOTH: Save to redis and mongoDB
 * NONE: Use only local cache (RAM)
 *
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
public enum OrganizationType {

	SYNC, SAVE_TO_DB, BOTH, NONE;
	
}
