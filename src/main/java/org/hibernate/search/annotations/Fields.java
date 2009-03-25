// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Mark a property as indexable into different fields
 * Useful if the field is used for sorting and searching
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.FIELD} )
@Documented
public @interface Fields {
	/**
	 * Fields
	 */
	Field[] value();

}
