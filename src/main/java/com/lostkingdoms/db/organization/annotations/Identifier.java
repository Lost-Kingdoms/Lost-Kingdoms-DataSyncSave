package com.lostkingdoms.db.organization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the identifier of a {@link OrganizedEntity}
 *
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Identifier {

}
