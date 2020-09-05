package com.lostkingdoms.db.organization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lostkingdoms.db.converters.impl.DefaultDataConverter;
import com.lostkingdoms.db.organization.enums.OrganizationType;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrganizedObject {
	
	String key();
	OrganizationType organizationType();
	Class<?> genericClass();
	Class<?> listType();
	Class<?> mapType();
	String optionalSuperKey();
}
