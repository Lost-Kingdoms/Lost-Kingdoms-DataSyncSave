package com.lostkingdoms.db.organization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lostkingdoms.db.organization.enums.OrganizationType;

/**
 * Marks a class as a {@link OrganizedSuperentity}.
 * If a subclass is constructed the superclass {@link OrganizedObject}s get intitialized 
 * as defined in {@link OrganizedObject} annotation. If superclass itself gets constructed
 * the fields get initialized with {@link OrganizationType} = NONE 
 * (-> A instance of a pure superclass gets not saved in cache or database!)
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrganizedSuperentity {
	
}
