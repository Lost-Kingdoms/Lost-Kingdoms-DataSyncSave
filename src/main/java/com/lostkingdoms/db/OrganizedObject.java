package com.lostkingdoms.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrganizedObject {
	
	String key();
	OrganizationType organizationType();
	String optionalSuperKey();
}
