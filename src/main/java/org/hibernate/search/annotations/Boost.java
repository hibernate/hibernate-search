//$Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Apply a boost factor on a field or a whole entity
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.TYPE, ElementType.METHOD, ElementType.FIELD} )
@Documented
public @interface Boost {
	float value();
}
