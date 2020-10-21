package com.lostkingdoms.db.organization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lostkingdoms.db.organization.enums.OrganizationType;
import com.lostkingdoms.db.organization.objects.OrganizedListDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedMapDataObject;
import com.lostkingdoms.db.organization.objects.OrganizedSingleDataObject;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrganizedObject {
	/**
	 * Part of the total key which represents this fields key
	 * 
	 * @return
	 */
	String objectKey();
	
	/**
	 * How this {@link OrganizedObject} should be organized
	 * 
	 * @return
	 */
	OrganizationType organizationType();
	
	/**
	 * If this {@link OrganizedObject} is a {@link OrganizedSingleDataObject} you HAVE TO
	 * define the generic type of the {@link OrganizedSingleDataObject} here
	 * 
	 * @return
	 */
	Class<?> singleClass() default Object.class;
	
	/**
	 * If this {@link OrganizedObject} is a {@link OrganizedListDataObject} you HAVE TO
	 * define the generic type of the {@link OrganizedListDataObject} here
	 * 
	 * @return
	 */
	Class<?> listClass() default Object.class;
	
	/**
	 * If this {@link OrganizedObject} is a {@link OrganizedMapDataObject} you HAVE TO
	 * define the generic key type of the {@link OrganizedMapDataObject} here
	 * 
	 * @return
	 */
	Class<?> mapKeyClass() default Object.class;
	
	/**
	 * If this {@link OrganizedObject} is a {@link OrganizedMapDataObject} you HAVE TO
	 * define the generic value type of the {@link OrganizedMapDataObject} here
	 * 
	 * @return
	 */
	Class<?> mapValClass() default Object.class;
	
	/**
	 * If this {@link OrganizedObject} is a {@link OrganizedSingleDataObject} which 
	 * represents a boolean you can define a default value here which will be
	 * set if there is no value in the global cache or database (OPTIONAL)
	 * 
	 * @return
	 */
	String defaultBooleanValue() default "";
	
	/**
	 * If this {@link OrganizedObject} is a {@link OrganizedSingleDataObject} which 
	 * represents an enum you can define a default value here which will be
	 * set if there is no value in the global cache or database (OPTIONAL)
	 * @return
	 */
	String defaultEnumValue() default "";
	
	/**
	 * If this {@link OrganizedObject} is not part of a {@link OrganizedEntity}
	 * you have to define a superkey here (the superkey then is used to replace
	 * the missing {@link OrganizedEntity}s key)
	 * 
	 * @return
	 */
	String optionalSuperKey() default "";
}
