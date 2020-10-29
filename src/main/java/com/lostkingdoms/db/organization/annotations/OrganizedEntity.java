package com.lostkingdoms.db.organization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a {@link OrganizedEntity}
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrganizedEntity {

	/**
	 * The key this entity is identified with. Uses the lower case class name if not overridden
	 * 
	 * @return
	 */
	String entityKey() default "";
	
}
